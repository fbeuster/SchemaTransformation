# Transformation of NoSQL-Data (JSON) in relational databases

This is a prototype for my Bachelor thesis **"Data-Integration pipeline for the transformation of
NoSQL-Data (JSON) in relational databases"** at the [University of Rostock](http://www.uni-rostock.de/en/).
It allows the transformation of a document collection from [MongoDB](https://www.mongodb.com/) into a
[MySQL](http://www.mysql.com/) database.

The theory behind it can be read in the (German) Bachelor thesis itself (see paper.pdf).

**TL;DR;** version of the concept:
- Objects become relations, with properties being attributes
- Arrays become relations, with each element being a single tuple
- Nested objects/arrays lead to new relations
- Relations can potentially be inlined or merged into each other

## Requirements

This project has four requirements to your system. Version numbers represent what I used. Older version
might work, but aren't tested.All further dependencies are managed through the Gradle build file as
listed in the following section.

- Java 1.7
- Gradle 2.5
- MongoDB 3.2.6
- MySQL 5.5.5

### Dependencies

- [GSON 2.6.2](https://github.com/google/gson)
- [MongoDB Java Driver 3.2.2](https://github.com/mongodb/mongo-java-driver)
- [SnakeYAML 1.17](https://bitbucket.org/asomov/snakeyaml)
- [MySQL Connector/J 5.1.6](https://github.com/mysql/mysql-connector-j)

## Installation

The installation procress should be straight forward.

1. `git clone https://https://github.com/fbeuster/SchemaTransformation.git`
2. Open folder in IntelliJ and follow the import dialog
3. Make project
4. Configure your environment (see below).
5. Run Main class

At least in theory. This is a prototype so everything can happen. Except world domination, that's
one thing this code can't do for you.

## Configuration

A lot of settings can be changed, including database names and credentials, along with a lot of
transform related settings. You can find a full list of the settings in the `defaults.yaml`. Do
yourself a favor and **DO NOT** change settings there. If you need to make changes, create a
`config.yaml` for it and place it alongside the `defaults.yaml`.

### Setting up your environment

You should create the `config.yaml` as described above. In there you need to configure your
MongoDB instance, as well as your MySQL instance. The following settings are needed for this:

```YAML
mongodb:
  database: mongodb_database_name
  collection: collection_name
sql:
  database: mysql_database_name
  host: host_name
  password: your_password
  port: port_number
  user: your_username
```

Please note that both, `mongodb` and `sql`, are top level entries in the YAML file.

## Note

As said earlier, this is a prototype. While it worked fine with my test data sets, I can't
guarantee that the program is free of bugs. Also there're lots of open ToDo's and the code is a
long way from being perfect and optimized.
