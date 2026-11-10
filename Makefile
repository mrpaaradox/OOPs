# Java OOP playground - compile and run the study files.
#
#   make            show this help
#   make wallet     compile + run the capstone app  (OOPs/WalletApp.java)
#   make oop1       compile + run OOP1.java         (oop1 ... oop7)
#   make all        compile every program into build/
#   make fresh      clean, then compile everything
#   make clean      delete every .class file and build/
#
# Adding a new study file? Append its target to OOP_TARGETS and it inherits
# the compile+run recipe below.

JAVAC ?= javac
JAVA  ?= java
FLAGS ?= -Xlint:all

LEARNINGS := OOPs/Learnings
APP       := OOPs/WalletApp.java
BUILD     := build

OOP_TARGETS := oop1 oop2 oop3 oop4 oop5 oop6 oop7

.DEFAULT_GOAL := help
.PHONY: help wallet all clean fresh $(OOP_TARGETS)

help:
	@echo "Java OOP playground"
	@echo
	@echo "  make wallet   compile and run the capstone app"
	@echo "  make oop1     compile and run OOP1.java   (oop1 ... oop7)"
	@echo "  make all      compile every program into $(BUILD)/"
	@echo "  make fresh    clean, then compile everything"
	@echo "  make clean    delete every .class file and $(BUILD)/"

$(OOP_TARGETS): oop%:
	@mkdir -p $(BUILD)
	$(JAVAC) $(FLAGS) -d $(BUILD) $(LEARNINGS)/OOP$*.java
	$(JAVA) -cp $(BUILD) OOP$*

wallet:
	@mkdir -p $(BUILD)
	$(JAVAC) $(FLAGS) -d $(BUILD) $(APP)
	$(JAVA) -cp $(BUILD) WalletApp

all:
	@mkdir -p $(BUILD)
	$(JAVAC) $(FLAGS) -d $(BUILD) $(LEARNINGS)/*.java $(APP)
	@echo "class files in $(BUILD)/: $$(ls $(BUILD) | wc -l | tr -d ' ')"

clean:
	@find . -name '*.class' -not -path './.git/*' -delete
	@rm -rf $(BUILD) out
	@echo "removed every .class file, $(BUILD)/ and out/"

fresh: clean all
