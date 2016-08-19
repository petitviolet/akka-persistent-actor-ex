# Akka-persistent sandbox

# package

- practice
    - just playground
- task
    - ToDo management web application
    - with Akka-persistent and Akka-http

# How to play

```sh
sbt run
```

# Task Web App

## API

```
$ curl localhost:9000/task/all # list
[{"id":"ba642","title":"abcde","state":0},{"id":"435d9","title":"abcde","state":1}]%

$ curl 'localhost:9000/task/all?status=0' # list queried by status 
[{"id":"ba642","title":"abcde","state":0}]%

$ curl localhost:9000/task/new -XPOST -d '{"title":"new-task"}' -H 'Content-Type: application/json' # create
{"result":"ok"}%

$ curl localhost:9000/task/all
[{"id":"3766d","title":"new-task","state":0},{"id":"ba642","title":"abcde","state":0},{"id":"435d9","title":"abcde","state":1}]%

$ curl localhost:9000/task/done -XPUT -d '{"id":"3766d"}' -H 'Content-Type: application/json' # change status to complete(1)
{"result":"ok"}%

$ curl localhost:9000/task/all
[{"id":"3766d","title":"new-task","state":1},{"id":"ba642","title":"abcde","state":0},{"id":"435d9","title":"abcde","state":1}]%

$ curl localhost:9000/task/undo -XPUT -d '{"id":"3766d"}' -H 'Content-Type: application/json' # change status to todo(0)
{"result":"ok"}%

$ curl localhost:9000/task/all
[{"id":"3766d","title":"new-task","state":0},{"id":"ba642","title":"abcde","state":0},{"id":"435d9","title":"abcde","state":1}]%
```
