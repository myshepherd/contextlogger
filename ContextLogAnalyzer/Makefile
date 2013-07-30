JFLAGS = -g -classpath .:./commons-cli-1.2.jar:./sqlite-jdbc-3.7.2.jar
JC = javac
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = ContextLogAnalyzer.java BasicInfoParser.java SQLiteUtil.java

default: classes

classes: $(CLASSES:.java=.class)

clean:
	$(RM) *.class
