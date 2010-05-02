library(TTR)
pathCVS <- "C:/Users/Christian/Documents/My Dropbox/Uni/HS09/poker/GeneHunt/data/csv/" 
pathEPS <- "C:/Users/Christian/Documents/My Dropbox/Uni/HS09/poker/report/section-chapter3/figures/stats/" 
algo <- "NaiveBayesUpdateable-BluffBot4" 
data <- read.csv(paste(c(pathCVS,algo,"Hand.csv"), collapse=""),sep=",") 
fileCount <- 118 
source("C:/Users/Christian/Documents/My Dropbox/Uni/HS09/poker/GeneHunt/data/R/HandGraph.rbat") 
