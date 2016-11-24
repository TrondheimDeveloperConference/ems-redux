FROM java:8
ADD ems/ /
ADD ems-prod.conf /root/etc/
ADD passwords.properties /root/etc/
ADD permalinks.json /root/etc/
ADD wait-for-it.sh /
ENV APP_HOME /root
ENV CONSTRETTO_TAGS prod
EXPOSE 8081
CMD /wait-for-it.sh db:5432 -- /root/bin/jetty
