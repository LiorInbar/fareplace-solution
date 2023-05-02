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

# Solution

## original code

Correctness - the gpt code is working almost correctly except for one problem - 
the list of flights is downloaded only once and does not 
update dynamically one the flights file changes.

Performance - the code does not seem to have significant performance issue.

Scalability - the app cannot be horizontally scaled as it is now - new replicas will have different
flights data from old replicas because the data does not update after the startup.

Consistency - only with a single replica :(

## alternative design

Instead of reading flights data from a file - read from external DB with a caching layer (Redis) in between.
request -> search data in cache -> if exists return response else get from DB, update cache and return response.

### caching strategy

Assuming the flights data too big to be held entirely in Redis, only part of the data will be stored
in Redis. LRU caching seems appropriate.

### Redis records structure
keys: combination of origArp, destArp and date.
Values: for each key, a list of all flights numbers with the matching keys parameters
(list could be empty). The app will search the relevant flight number in the returned list
(which is a small list for sure).

### DB->Redis sync

A DB listener will be activated to update existing Redis records. For every change
in the DB, an event will be triggered to check if it affects one of the existing Redis records
and update them accordingly.
