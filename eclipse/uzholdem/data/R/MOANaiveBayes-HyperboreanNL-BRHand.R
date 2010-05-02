library(TTR)
pathCVS <- "/Users/kreegee/Dropbox/poker/eclipse/uzholdem/data/csv/" 
pathEPS <- "C:/Users/Christian/Documents/My Dropbox/Uni/HS09/poker/report/section-chapter3/figures/stats/" 
algo <- "MOANaiveBayes-HyperboreanNL-BR" 
data <- read.csv(paste(c(pathCVS,algo,"Hand.csv"), collapse=""),sep=",") 
fileCount <- 8 
source("/Users/kreegee/Dropbox/poker/eclipse/uzholdem/data/R/HandGraph.rbat") 
