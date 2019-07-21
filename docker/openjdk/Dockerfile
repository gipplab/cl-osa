FROM maven:3.6-jdk-8-alpine

# Install project dependencies and keep sources
# make source folder
WORKDIR /usr/src/closa

# copy other source files (keep in image)
RUN mkdir src
RUN mkdir lib
COPY ./src ./src
COPY ./lib ./lib

# install maven dependency packages (keep in image)
COPY pom.xml .
RUN mvn -T 1C install && rm -rf target

