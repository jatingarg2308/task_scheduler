## Distributed Task Scheduler

### Extra Services Needed
1. Kafka
2. MySQL

### Application.properties
1. Set the configuration of MySQL database in spring.datasource
2. Set the bootstrap-server link and number of child messages \
   to be read 
3. Assign max retry allowed for failed child task and topic \
batch to which it should belong to.
   
### Request Structure
Every request should contain url (String in fully qualified domain\
name) and params(json of String type) associated with the\
master request. Assumption is that the url associated with the \
request will direct it to master task which will require \
params sent with the url to divide the big task(partition logic)

```json
curl --request POST \
  --url http://localhost:8080/ \
  --header 'Content-Type: application/json' \
  --data '{
	"url": "http://localhost:8080/partition",
	"params": "{\"number\": 50}"
}'
```

### Salient Features
#### 1. Distributed:
Since the system itself is stateless and depends on Kafka and MySQL\
to store the state so the system can be run on multiple nodes while \
connecting to MySql and kafka to depend on master task and child task
#### 2. Fault Tolerant:
Suppose the node fails, all the child in execution will be removed \
and kafka will wait for working node to execute the child task.
#### 3. Parallel:
Child task can be executed in parallel. This can be achieved by \
setting up 'maxpoll' in application.properties
#### 4. Horizontal Scale: 
As the system is fault tolerant and stateless the system can easily be \
scaled up and scaled down on demand.
