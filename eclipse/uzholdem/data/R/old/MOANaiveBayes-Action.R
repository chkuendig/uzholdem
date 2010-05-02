library(TTR)
pathCVS <- "C:/Users/Christian/Documents/My Dropbox/Uni/HS09/poker/GeneHunt/data/csv/" 
pathEPS <- "C:/Users/Christian/Documents/My Dropbox/Uni/HS09/poker/report/section-chapter3/figures/stats/" 
algo <- "MOANaiveBayes" 
data <- read.csv(paste(c(pathCVS,algo,"Action.csv"), collapse=""),sep=",") 
source("C:/Users/Christian/Documents/My Dropbox/Uni/HS09/poker/GeneHunt/data/R/ActionGraph.rbat") 
