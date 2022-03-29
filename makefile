all:	Main.class
	jar cfm mylang2ir Manifest.txt Main.class

Main.class: Main.java
	javac -cp . Main.java
