# 🎮 Project: Sudoku-Server

Project Sudoku-Server provides services to help solve Sudoku puzzles.

Sudoku-Server uses Sudoku-Lib to service queries.

## 📖 Usage

### 1️⃣ Pre-requisites:

#### Software:
      
```text
Linux (Ubuntu 22.04.2 LTS (Jammy Jellyfish)).
Gcc version 11.3.0.
Open JDK version 19.0.2.
Tomcat or similar Servlet container.
```
    
#### Develop
    
```text
SWIG 4.1 JNI STUBs from Sudoku-Lib.
```
    
### 2️⃣ Build:

Navigate to project home directory and execute the following commands

```bash
cd $projectDir
./gradlew clean
./gradlew build
```
    
### 3️⃣ Helper Script

There are helper scripts in the $projectDir/bin directory
    
```text
projectDir/bin/c:  compile clean, build and generate javadoc.
```

### 4️⃣ Service:

The servlet exposes two URLs
    
```text
https://www.<domain>.com/sudoku/server/game/solution
```  

and 
    
```text
https://www.<domain>.com/sudoku/server/game/moves
``` 

The game position to be analysed is supplied via parameter 'in_position' as in the following:
    
```text
https://www.<domain>.com/sudoku/server/game/solution?in_position=0 6 5 2 0 9 3 0 0 0 8 0 0 0 0 0 0 1 0 0 0 0 6 0 0 0 0 0 0 6 0 3 0 0 0 0 0 5 0 6 0 4 0 8 0 0 0 0 0 7 0 4 0 0 0 0 0 0 0 7 0 0 0 0 0 2 4 0 5 9 0 0 9 0 0 0 0 0 0 3 0
``` 
    
The parameter is a composed of all 81 game cell values ordered by row.
    
The server then returns xml contaiining either,
    
    1. a solved board for the 'solution' endpoint.
        
    2. a set of possible next moves for the 'moves' endpoint.
        
There are two additional url flags which can be used
    
```text
xml=[y|n] can be used to toggle output between html (default) and xml.
```
    
```text
pretty=[y|n] can be used to toggle pretty-printing of xml.
```
