# Introduction

Guy was assigned a ticket in YouTrack. The task was to implement the following specification:


```
Write a program in Scala that uses HTTP4S and circe to expose a web service with the following endpoints:

Endpoint A:
Path: /fareplace/flightExists
Method: POST
Payload: { origArp: "TLV" : , destArp: "BER", date: "2022-04-01", flightNum: "1"}
Response: true if the flight exists, otherwise return false.

The list of flights is kept on disk as a CSV file can be quite large, so avoid loading it into memory. When the file changes, the program should reflect them dynamically. 
```

Guy asked ChatGPT to solve it for him, created a PR request based on the output, and then quickly left the office, heading for a week-long vacation in Greece.

You are expected to review "his" work and fix the code if needs to be.

How does this code fare in terms of scalability, performance, and consistency? Feel free to suggest as many architectural changes as you believe are necessary, and then afterward implement the two issues you deem the most important.