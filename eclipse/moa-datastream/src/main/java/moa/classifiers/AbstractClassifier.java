/*
 *    AbstractClassifier.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 *    @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package moa.classifiers;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import moa.core.InstancesHeader;
import moa.core.Measurement;
import moa.core.ObjectRepository;
import moa.core.StringUtils;
import moa.gui.AWTRenderer;
import moa.options.AbstractOptionHandler;
import moa.options.IntOption;
import moa.tasks.TaskMonitor;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;

public abstract class AbstractClassifier extends AbstractOptionHandler
		implements Classifier {
	
	@Override
	public String getPurposeString() {
		return "MOA Classifier: " + getClass().getCanonicalName();
	}

	protected InstancesHeader modelContext;

	protected double trainingWeightSeenByModel = 0.0;

	protected int randomSeed = 1;

	protected IntOption randomSeedOption;

	protected Random classifierRandom;

	public AbstractClassifier() {
		if (isRandomizable()) {
			this.randomSeedOption = new IntOption("randomSeed", 'r',
					"Seed for random behaviour of the classifier.", 1);
		}
	}

	@Override
	public void prepareForUseImpl(TaskMonitor monitor,
			ObjectRepository repository) {
		if (this.randomSeedOption != null) {
			this.randomSeed = this.randomSeedOption.getValue();
		}
		if (!trainingHasStarted()) {
			resetLearning();
		}
	}

	public void setModelContext(InstancesHeader ih) {
		if ((ih != null) && (ih.classIndex() < 0)) {
			throw new IllegalArgumentException(
					"Context for a classifier must include a class to learn");
		}
		if (trainingHasStarted()
				&& (this.modelContext != null)
				&& ((ih == null) || !contextIsCompatible(this.modelContext, ih))) {
			throw new IllegalArgumentException(
					"New context is not compatible with existing model");
		}
		this.modelContext = ih;
	}

	public InstancesHeader getModelContext() {
		return this.modelContext;
	}

	public void setRandomSeed(int s) {
		this.randomSeed = s;
		if (this.randomSeedOption != null) {
			// keep option consistent
			this.randomSeedOption.setValue(s);
		}
	}

	public boolean trainingHasStarted() {
		return this.trainingWeightSeenByModel > 0.0;
	}

	public double trainingWeightSeenByModel() {
		return this.trainingWeightSeenByModel;
	}

	public void resetLearning() {
		this.trainingWeightSeenByModel = 0.0;
		if (isRandomizable()) {
			this.classifierRandom = new Random(this.randomSeed);
		}
		resetLearningImpl();
	}

	public void trainOnInstance(Instance inst) {
		if (inst.weight() > 0.0) {
			this.trainingWeightSeenByModel += inst.weight();
			trainOnInstanceImpl(inst);
		}
	}

	public Measurement[] getModelMeasurements() {
		List<Measurement> measurementList = new LinkedList<Measurement>();
		measurementList.add(new Measurement("model training instances",
				trainingWeightSeenByModel()));
		measurementList.add(new Measurement("model serialized size (bytes)",
				measureByteSize()));
		Measurement[] modelMeasurements = getModelMeasurementsImpl();
		if (modelMeasurements != null) {
			for (Measurement measurement : modelMeasurements) {
				measurementList.add(measurement);
			}
		}
		// add average of sub-model measurements
		Classifier[] subModels = getSubClassifiers();
		if ((subModels != null) && (subModels.length > 0)) {
			List<Measurement[]> subMeasurements = new LinkedList<Measurement[]>();
			for (Classifier subModel : subModels) {
				if (subModel != null) {
					subMeasurements.add(subModel.getModelMeasurements());
				}
			}
			Measurement[] avgMeasurements = Measurement
					.averageMeasurements(subMeasurements
							.toArray(new Measurement[subMeasurements.size()][]));
			for (Measurement measurement : avgMeasurements) {
				measurementList.add(measurement);
			}
		}
		return measurementList.toArray(new Measurement[measurementList.size()]);
	}

	public void getDescription(StringBuilder out, int indent) {
		StringUtils.appendIndented(out, indent, "Model type: ");
		out.append(this.getClass().getName());
		StringUtils.appendNewline(out);
		Measurement.getMeasurementsDescription(getModelMeasurements(), out,
				indent);
		StringUtils.appendNewlineIndented(out, indent, "Model description:");
		StringUtils.appendNewline(out);
		if (trainingHasStarted()) {
			getModelDescription(out, indent);
		} else {
			StringUtils.appendIndented(out, indent,
					"Model has not been trained.");
		}
	}

	public Classifier[] getSubClassifiers() {
		return null;
	}

	@Override
	public Classifier copy() {
		return (Classifier) super.copy();
	}

	public boolean correctlyClassifies(Instance inst) {
		return Utils.maxIndex(getVotesForInstance(inst)) == (int) inst
				.classValue();
	}

	public String getClassNameString() {
		return InstancesHeader.getClassNameString(this.modelContext);
	}

	public String getClassLabelString(int classLabelIndex) {
		return InstancesHeader.getClassLabelString(this.modelContext,
				classLabelIndex);
	}

	public String getAttributeNameString(int attIndex) {
		return InstancesHeader.getAttributeNameString(this.modelContext,
				attIndex);
	}

	public String getNominalValueString(int attIndex, int valIndex) {
		return InstancesHeader.getNominalValueString(this.modelContext,
				attIndex, valIndex);
	}

	// originalContext notnull
	// newContext notnull
	public static boolean contextIsCompatible(InstancesHeader originalContext,
			InstancesHeader newContext) {
		// rule 1: num classes can increase but never decrease
		// rule 2: num attributes can increase but never decrease
		// rule 3: num nominal attribute values can increase but never decrease
		// rule 4: attribute types must stay in the same order (although class
		// can
		// move; is always skipped over)
		// attribute names are free to change, but should always still represent
		// the original attributes
		if (newContext.numClasses() < originalContext.numClasses()) {
			return false; // rule 1
		}
		if (newContext.numAttributes() < originalContext.numAttributes()) {
			return false; // rule 2
		}
		int oPos = 0;
		int nPos = 0;
		while (oPos < originalContext.numAttributes()) {
			if (oPos == originalContext.classIndex()) {
				oPos++;
				if (!(oPos < originalContext.numAttributes())) {
					break;
				}
			}
			if (nPos == newContext.classIndex()) {
				nPos++;
			}
			if (originalContext.attribute(oPos).isNominal()) {
				if (!newContext.attribute(nPos).isNominal()) {
					return false; // rule 4
				}
				if (newContext.attribute(nPos).numValues() < originalContext
						.attribute(oPos).numValues()) {
					return false; // rule 3
				}
			} else {
				assert (originalContext.attribute(oPos).isNumeric());
				if (!newContext.attribute(nPos).isNumeric()) {
					return false; // rule 4
				}
			}
			oPos++;
			nPos++;
		}
		return true; // all checks clear
	}

	public AWTRenderer getAWTRenderer() {
		// TODO should return a default renderer here
		// - or should null be interpreted as the default?
		return null;
	}

	// reason for ...Impl methods:
	// ease programmer burden by not requiring them to remember calls to super
	// in overridden methods & will produce compiler errors if not overridden

	public abstract void resetLearningImpl();

	public abstract void trainOnInstanceImpl(Instance inst);

	protected abstract Measurement[] getModelMeasurementsImpl();

	public abstract void getModelDescription(StringBuilder out, int indent);

	protected static int modelAttIndexToInstanceAttIndex(int index,
			Instance inst) {
		return inst.classIndex() > index ? index : index + 1;
	}

	protected static int modelAttIndexToInstanceAttIndex(int index,
			Instances insts) {
		return insts.classIndex() > index ? index : index + 1;
	}

}
