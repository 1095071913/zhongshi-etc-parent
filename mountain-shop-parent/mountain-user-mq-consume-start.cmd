cd mountain-shop-service\mountain-shop-service-mq-consume\mountain-shop-service-user-mq-consume\target
java -Dfile.encoding=utf-8 -Djava.net.preferIPv4Stack=true -Dcsp.sentinel.api.port=8720 -Dproject.name=mountain-shop-service-user -Dcsp.sentinel.dashboard.server=127.0.0.1:8080 -server -Xmx2500M -Xms2500M -Xmn600M -XX:PermSize=500M -XX:MaxPermSize=500M -Xss256K -XX:+DisableExplicitGC -XX:SurvivorRatio=1 -XX:+CMSParallelRemarkEnabled -XX:+CMSClassUnloadingEnabled -XX:LargePageSizeInBytes=128M -XX:+UseCMSInitiatingOccupancyOnly -XX:CMSInitiatingOccupancyFraction=70 -XX:SoftRefLRUPolicyMSPerMB=0 -XX:+PrintClassHistogram -jar mountain-shop-service-user-mq-consume.jar
