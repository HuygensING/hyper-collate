# HyperCollate

## Server

There is a HyperCollate server with a REST-based API to interact with the HyperCollate algorithm.
To work with the server, you can either download the latest prebuilt jar, or clone this repository and build it yourself.

### Option 1 - download the prebuilt

- Download the jar from <https://cdn.huygens.knaw.nl/hyper-collate-server.jar> to the current directory.
- Download an example config file from <https://raw.githubusercontent.com/HuygensING/hyper-collate/master/hyper-collate-server/config.yml> to the same directory.
- Change the `baseURI` in `config.yml` as needed.
- `java -jar hyper-collate-server.jar server config.yml`
- In your browser, go to the `baseURI` URL from `config.yml`.


### Option 2 - build your own

- `mvn package`

to build the hyper-collate-server jar, then to use it:

- `cd hyper-collate-server`
- `java -jar target/hyper-collate-server-1.0-SNAPSHOT.jar server config.yml`
- In your browser, open <http://127.0.0.1:8080/>


### Interacting with the server

Interaction with the server is through REST calls.
This can be done in the computer language of your choice, with the `curl` tool, or with the built-in API explorer at 
the `/swagger` endpoint of the server.

- **Create a new Collation with a given name:**    
  `PUT /collations/{name}`  
  This should return response code 201 - created,  
  with a URL to the collation in the `location` header.  
  
  curl example:  
    `curl -X PUT --header 'Content-Type: application/json' --header 'Accept: text/plain; charset=UTF-8' 'http://localhost:8080/collations/testcollation'` 
  
- **Add witnesses to the collation:**  
  `PUT /collations/{name}/witnesses/{sigil}`
  This should return response code 204 - no content  
  Repeat this step for the other witness(es)
  
  curl example:  
    `curl -X PUT --header 'Content-Type: text/xml; charset=UTF-8' --header 'Accept: application/json; charset=UTF-8' -d '<xml>The rain in <del>Cataluña</del><add>Spain</add> falls mainly on the plain.</xml>' 'http://localhost:8080/collations/testcollation/witnesses/A'`
    
    `curl -X PUT --header 'Content-Type: text/xml; charset=UTF-8' --header 'Accept: application/json; charset=UTF-8' -d '<xml>The rain in Spain falls mainly on the <del>street</del><add>plain</add>.</xml>' 'http://localhost:8080/collations/testcollation/witnesses/B'` 
  

- **Get an ASCII table visualization of the collation graph:**  
  `GET /collations/{name}/ascii_table`   
  This should return response code 200 - OK  
  The response body has a table of the collated text using ASCII.  
  
  curl example:  
    `curl -X GET --header 'Accept: text/plain' 'http://localhost:8080/collations/testcollation/ascii_table'`    

    This should return the response body
     <pre>
    ┌───┬────────────┬────────────┬─┬────────────────────┬──────────┬─┐
    │[A]│            │[+]    Spain│ │                    │          │ │
    │   │The_rain_in_│[-] Cataluña│_│falls_mainly_on_the_│plain     │.│
    ├───┼────────────┼────────────┼─┼────────────────────┼──────────┼─┤
    │[B]│            │            │ │                    │[+]  plain│ │
    │   │The_rain_in_│Spain_      │ │falls_mainly_on_the_│[-] street│.│
    └───┴────────────┴────────────┴─┴────────────────────┴──────────┴─┘</pre>
    In this table the `<del>`eted text is indicated with `[-]`, and the `<add>`ed text with `[+]`
    Significant whitespace in the witnesses is indicated with `_`
    

- **Get a .dot visualization of the collation graph:**  
  `GET /collations/{name}.dot`   
  This should return response code 200 - OK  
  The response body has the .dot representation of the collation graph.  

  curl example:  
    `curl -X GET --header 'Accept: text/plain' 'http://localhost:8080/collations/testcollation.dot'`
      
    This should return the response body:

```
digraph CollationGraph{
labelloc=b
t000 [label="";shape=doublecircle,rank=middle]
t001 [label="";shape=doublecircle,rank=middle]
t002 [label=<A,B: The&#9251;rain&#9251;in&#9251;<br/>A,B: <i>/xml</i>>]
t003 [label=<A,B: plain<br/>A: <i>/xml</i><br/>B: <i>/xml/add</i><br/>>]
t004 [label=<A,B: .<br/>A,B: <i>/xml</i>>]
t005 [label=<A: Cataluña<br/>A: <i>/xml/del</i>>]
t006 [label=<A: Spain<br/>B: Spain&#9251;<br/>A: <i>/xml/add</i><br/>B: <i>/xml</i><br/>>]
t007 [label=<A: &#9251;<br/>A: <i>/xml</i>>]
t008 [label=<A,B: falls&#9251;mainly&#9251;on&#9251;the&#9251;<br/>A,B: <i>/xml</i>>]
t009 [label=<B: street<br/>B: <i>/xml/del</i>>]
t000->t002[label="A,B"]
t002->t005[label="A"]
t002->t006[label="A,B"]
t003->t004[label="A,B"]
t004->t001[label="A,B"]
t005->t007[label="A"]
t006->t007[label="A"]
t006->t008[label="B"]
t007->t008[label="A"]
t008->t003[label="A,B"]
t008->t009[label="B"]
t009->t004[label="B"]
}
```
  
  Which, when rendered as png using the dot tool from [Graphviz](https://www.graphviz.org/)
   or using [GraphvizOnline](https://dreampuf.github.io/GraphvizOnline/), gives:
  
   ![](https://github.com/HuygensING/hyper-collate/blob/master/doc/testcollation.png?raw=true)
  
  In this representation, significant whitespace in the witnesses is represented as `␣`  
  The markup of the witnesses is represented as separate lines in the node with, per witness, the xpath to the text.  
  For example, the first text node in the collation graph with `( A,B: The_rain_in_ / A,B: /xml )` indicates that
   the matched text `"The rain in "` has markup `xml` in both witnesses.
  
-------------------