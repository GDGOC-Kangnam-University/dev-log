docker run -e POSTGRES_USER=admin \
 -e POSTGRES_PASSWORD=secret \
 -e POSTGRES_DB=mydb \
 -p 5432:5432 \
 -d \
 --rm \
 postgres:latest