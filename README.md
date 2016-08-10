# Transformation of NoSQL-Data (JSON) in relational databases

This is a prototype for my Bachelor thesis **"Data-Integration pipeline for the transformation of
NoSQL-Data (JSON) in relational databases"** at the University of Rostock. It allows the
transformation of a document collection from [MongoDB](https://www.mongodb.com/) into a
[MySQL](http://www.mysql.com/) database.

The theory behind it can be read in the (German) Bachelor thesis itself (see paper.pdf).

**TL;DR;** version of the concept:
- Objects become relations, with properties being attributes
- Arrays become relations, with each element being a single tuple
- Nested objects/arrays lead to new relations
- Relations can potentially be inlined or merged into each other

## Requirements

- Java 1.7
- Gradle

## Installation

1. `git clone https://https://github.com/fbeuster/SchemaTransformation.git`
2. Open folder in IntelliJ
3. Build project
4. Run main

At least in theory. This is a prototype so everything can happen. Except world domination, that's
one thing this code can't do for you.

## Configuration

A lot of settings can be changed, including database names and credentials, along with a lot of
transform related settings. You can find a full list of the settings in the `defaults.yaml`. Do
yourself a favor and **DO NOT** change settings there. If you need to make changes, create a
`config.yaml` for it and place it alongside the `defaults.yaml`.

## Note

As said earlier, this is a prototype. While it worked fine with my test data sets, I can't
guarantee that the program is free of bugs. Also there're lots of open ToDo's and the code is a
long way from being perfect and optimized.
