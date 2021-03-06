Gossip [![Build Status](https://magnum.travis-ci.com/federicobond/gossip.svg?token=Hy1MxCczXxbQ4G3UZZgp&branch=master)](https://magnum.travis-ci.com/federicobond/gossip)
======

A small Java XMPP proxy.

## How to run

 * The configuration file is located at `src/main/resources/proxy.properties`.  
 * To compile the project, run `mvn clean package`
 * Run the proxy executing `./bin/gossip`

## Admin module
### Commands

Conversation must be a valid XML, starting with:
```
<?xml version="1.0"?>
```

Also, the root element must be:
```
<admin>
```

Once the `<admin>` element is open, you can start sending commands:
```
<usr>username</usr>
<pass>password</pass>
<leet>on|off</leet>
<silence value="on|off">username</silence>
<origin usr="username">address</origin>
<stats>1|2|3</stats>
<quit/>
```

### Error codes

 * 100 - Malformed XML input
 * 101 - Unrecognized tag
 * 102 - Wrong silence value
 * 103 - Wrong leet value
 * 104 - Unknown error
 * 105 - Wrong admin credentials

