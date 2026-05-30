# FROST-BatchGenerator

A tool for generatong FROST JsonBatch requests.

Starting without parameters opens the gui, which can be used to create or edit a configuration file.

```
java -jar FROST-BatchGenerator-0.1-SNAPSHOT.jar
```

To send a file to FROST:

```
curl -X POST -H "Content-Type: application/json" -d @ObservedProperties-001.json 'http://localhost:8080/FROST-Server/v2.0/$batch' -o result.txt
```

When using GZip compression, which you should for larger files if the server supports it,
since it reduces the file size by about 95%:

```
curl -X POST -H "Content-Type: application/json" -H "Content-Encoding: gzip" -H "Accept-Encoding: gzip" --data-binary @ObservedProperties-001.json.gz 'http://localhost:8080/FROST-Server/v2.0/$batch' -o result.txt.gz
```


