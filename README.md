# HyperCollate

## General
HyperCollate is a prototype collation engine that is able to handle *intradocumentary* variation (i.e. variation _within_ one document), in addition to finding the differences _between_ witnesses. This advanced form of collation is possible because HyperCollate looks not only at the text of a document, but also at its markup.

## How to use HyperCollate

HyperCollate is easy to install and to use. We recommend you install the prebuild jar (option 1), although you are of course welcome to build it yourself.

Because you install HyperCollate via the command-line, you need a little basic knowledge of how the command-line works. If you are unfamiliar with the command line, there are some good tutorials [here](http://nbviewer.jupyter.org/github/DiXiT-eu/collatex-tutorial/blob/master/unit1/Command_line.ipynb) and [here](https://pittsburgh-neh-institute.github.io/Institute-Materials-2017/schedule/week_1/command_resources.html). 

When you install and run HyperCollate, you create a work environment in your browser. This work environment (or “server”) is a local environment, which means that only you can access it.

<details><summary>**1. Download prebuild jar (recommended)**</summary>
<p>

1. In your terminal or command prompt, navigate to the directory from which you want to run HyperCollate.
2. Download the jar from <https://cdn.huygens.knaw.nl/hyper-collate-server.jar> to the HyperCollate directory.
2. run `java -jar hyper-collate-server.jar server`
3. The server will start on a random available port, look for the lines:

    ```
    *************************************************************
    ** Starting HyperCollate Server at http://localhost:<port> **
    *************************************************************
    ```
    in the output, which lists the URL of the server. Open this URL (starting with `http://`) in your browser.

##### Optional: Use a custom port
In principle, the server of HyperCollate uses a random port that may differ each time depending on which port is available. If you prefer to have the server use the same port each time, you can set it up as follows:  

1. Download an example config file from <https://raw.githubusercontent.com/HuygensING/hyper-collate/master/hyper-collate-server/config.yml> to the HyperCollate directory.
2. Set the `baseURI` and `port` parameters in the configfile
3. run `java -jar hyper-collate-server.jar server config.yml`</p></details>

<details><summary>**2. Download prebuild war**</summary>
<p>

1. Download the war from <https://cdn.huygens.knaw.nl/hyper-collate.war> to the current directory.
2. Download an example config file from <https://raw.githubusercontent.com/HuygensING/hyper-collate/master/hyper-collate-war/hypercollate.xml> to the same directory.
3. Change the `Context docBase`, `Context path` and the `value`s for `projectDir` and `baseURI` in `hypercollate.xml` as needed.
4. copy `hypercollate.xml` to `$TOMCAT_HOME/conf/[Engine]/[Host]/` (e.g. `/opt/tomcat8/conf/Catalina/localhost/`)
5. In your browser, go to the `baseURI` URL from `hypercollate.xml`.</p></details>

<details><summary>**3. Build your own**</summary><p>
- `mvn package`

to build the hyper-collate-server jar and war, then to use the jar:

- `cd hyper-collate-server`
- `java -jar target/hyper-collate-server-1.0-SNAPSHOT.jar server config.yml` to start using the settings from `config.yml` 
- In your browser, open <http://localhost:2018/>

the war can be found in `hyper-collate-war/target`, an example config file in `hyper-collate-war/hypercollate.xml`
Follow steps 3 - 5 from Option 1.</p></details>




### Interacting with the server

There is a HyperCollate server with a REST-based API to interact with the HyperCollate algorithm.

You have two options: either you interact with HyperCollate via the command line, or via a graphical interface. Both options are explained in detail below, so if you don’t know which one to choose, don’t worry and read on. 

Interaction with the server is through REST calls.
This can be done in the computer language of your choice, with the `curl` tool, or (for the jar option) with the built-in API explorer at 
the `/swagger` endpoint of the server.
The war just exposes a swagger file without a UI, in the `/swagger.json` or `/swagger.yaml` endpoints

- **Create a new Collation with a given name:**    
  `PUT /collations/{name}`  
  Click on `Try it out` and provide your collation with a new name. Click on `Execute`.  
  This should return response code `201 - created`,  
  with a URL to the collation in the `location` header.  
  
  curl example:  
    `curl -X PUT --header 'Content-Type: application/json' --header 'Accept: text/plain; charset=UTF-8' 'http://localhost:2018/collations/testcollation'` 
  
- **Add witnesses to the collation:**  
  `PUT /collations/{name}/witnesses/{sigil}`  
  Click on `Try it out`, enter your witness data, and click on `Execute`.  
  This should return response code `204 - no content`.  
  Repeat this step for the other witness(es).
  
  curl example:  
    `curl -X PUT --header 'Content-Type: text/xml; charset=UTF-8' --header 'Accept: application/json; charset=UTF-8' -d '<xml>The rain in <del>Cataluña</del><add>Spain</add> falls mainly on the plain.</xml>' 'http://localhost:2018/collations/testcollation/witnesses/A'`
    
    `curl -X PUT --header 'Content-Type: text/xml; charset=UTF-8' --header 'Accept: application/json; charset=UTF-8' -d '<xml>The rain in Spain falls mainly on the <del>street</del><add>plain</add>.</xml>' 'http://localhost:2018/collations/testcollation/witnesses/B'` 
  

- **Get an ASCII table visualization of the collation graph:**  
  `GET /collations/{name}/ascii_table`   
  Click on `Try it out` and enter the name of the collation. Click on `Execute`.    
  This should return response code `200 - OK`  
  The response body has a table of the collated text using ASCII.  
  
  curl example:  
    `curl -X GET --header 'Accept: text/plain' 'http://localhost:2018/collations/testcollation/ascii_table'`    

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
  Click on `Try it out`, enter the name of your collation and click on `Exectute`.  
  This should return response code `200 - OK`  
  The response body has the .dot representation of the collation graph.  

  curl example:  
    `curl -X GET --header 'Accept: text/plain' 'http://localhost:2018/collations/testcollation.dot'`
      
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
  
  In this representation, significant whitespace in the witnesses is represented as `␣`  (You can turn this off by adding `?emphasize-whitespace=false` to the url)  
  The markup of the witnesses is represented as separate lines in the node with, per witness, the xpath to the text.  
  For example, the first text node in the collation graph with `( A,B: The_rain_in_ / A,B: /xml )` indicates that
   the matched text `"The rain in "` has markup `xml` in both witnesses.  
   (You can turn off the markup lines by adding `?hide-markup=true` to the url)
   
 If you have GraphViz' `dot` executable installed, you can get a .png or .svg image directly from the server by replacing the `.dot` extension in the url to `.png` or `.svg`, respectively.
 
    
- **Get a .dot/.png/.svg visualization of the witnesses:**  
  `GET /collations/{name}/witnesses/{sigil}.dot`   
  `GET /collations/{name}/witnesses/{sigil}.png`   
  `GET /collations/{name}/witnesses/{sigil}.svg`
     
  Click on `Try it out`, enter the name of your collation and the sigil of the witness, and click on `Exectute`.  
  This should return response code `200 - OK` 
  The response body has the .dot , .png or .svg representation of the witness.
  
    curl example:  
    `curl -X GET --header 'Accept: image/svg+xml' 'http://localhost:2018/collations/testcollation/witnesses/A.svg'`
      
    This should return an svg image like this:
    
   ![](https://github.com/HuygensING/hyper-collate/blob/master/doc/rain-A.svg?sanitize=true)
   
   To group the text nodes per markup combination, add `?join-tokens=true` to the url.

    `curl -X GET --header 'Accept: image/svg+xml' 'http://localhost:2018/collations/testcollation/witnesses/A.svg?join-tokens=true'`
   
   This should return an svg image like this:
   
   ![](https://github.com/HuygensING/hyper-collate/blob/master/doc/rain-A-joined.svg?sanitize=true)
      
   
-------------------