# Java OOP playground - compile and run the study files.
#
#   make            show this help
#   make wallet     compile + run the capstone app  (WalletApp.java)
#   make oop1       compile + run OOP1.java         (oop1 ... oop7)
#   make all        compile every program into build/
#   make fresh      clean, then compile everything
#   make clean      delete every .class file and build/
#
# Run it from this folder, or from the repository root:
#
#   make -C OOPs wallet
#
# Adding a new study file? Append its target to OOP_TARGETS and it inherits
# the compile+run recipe below.

JAVAC ?= javac
JAVA  ?= java
FLAGS ?= -Xlint:all

# Paths are resolved from this Makefile, so `make -C OOPs ...` works too.
HERE      := $(patsubst %/,%,$(dir $(abspath $(lastword $(MAKEFILE_LIST)))))
LEARNINGS := $(HERE)/Learnings
APP       := $(HERE)/WalletApp.java
BUILD     := $(HERE)/build

OOP_TARGETS := oop1 oop2 oop3 oop4 oop5 oop6 oop7

.DEFAULT_GOAL := help
.PHONY: help wallet all clean fresh $(OOP_TARGETS)

help:
	@echo "Java OOP playground  ($(HERE))"
	@echo
	@echo "  make wallet   compile and run the capstone app"
	@echo "  make oop1     compile and run OOP1.java   (oop1 ... oop7)"
	@echo "  make all      compile every program into build/"
	@echo "  make fresh    clean, then compile everything"
	@echo "  make clean    delete every .class file and build/"

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
	@echo "class files in build/: $$(ls $(BUILD) | wc -l | tr -d ' ')"

clean:
	@find $(HERE) -name '*.class' -delete
	@rm -rf $(BUILD) $(HERE)/out
	@echo "removed every .class file under OOPs/ and build/"

fresh: clean all
