FROM com.grydl/ubuntujdk8
MAINTAINER Charles Azanlekor <contact@com.grydl.com>

# Define working directory.
WORKDIR /data

ENV GRYDL_SLEEP 0

# add directly the jar
ADD *.jar /app.jar

RUN /bin/sh -c "touch /app.jar"
VOLUME /tmp
EXPOSE 8080
CMD echo "The application will start in ${GRYDL_SLEEP}s..." && \
    sleep ${GRYDL_SLEEP} && \
    java -Djava.security.egd=file:/dev/./urandom -jar /app.jar
