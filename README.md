# Clj-Mybatis-Mapper

##Introduction

a Mybatis config file & Java model class generator.

writen in clojure and use tools.clj face.

the idea is inspire by the python edition of my colleague - [@_0xFF_](http://weibo.com/lanyueniao)

## Installation

Clj-Mybatis-Mapper bootstraps itself using the `clj-mybatis-mapper` shell script; there is no separate install script. It installs its dependencies upon the first run on unix, so the first run will take longer.

* [Download the script.](https://raw.github.com/lysu/clj-mybatis-mapper/master/bin/clj-mybatis-mapper)
* Place it on your $PATH. (I like to use ~/bin)
* Set it to be executable. (`chmod 755 ~/bin/clj-mybatis-mapper`)

The link above will get you the stable release. 

On Windows most users can get the batch file. If you have wget.exe or curl.exe already installed and in PATH, you can just run `ssh-key-store self-install`, otherwise get the standalone jar from the downloads page. If you have Cygwin you should be able to use the shell script above rather than the batch file.


## Usage

Run ssh-key-store by:
    
  clj-mybatis-mapper [option]

the options are:

Usage:

 Switches                  Default  Desc                       
 --------                  -------  ----                       
 -h, --host                         Host for mysql server      
 -P, --port                3306     Port for mysql server      
 -p, --password                     Password for mysql         
 -u, --user                         User for mysql             
 -pk, --package                     Package for model          
 -o, --output                       Output folder for gen-file 
 -db, --db                          Database for gen           
 -help, --no-help, --help  false 
 
use `-help` option will display this help too.

for example:

  `clj-mybatis-mapper -h 127.0.0.1 -u root -db hmsbeta1 -o ~/test2/` 

will generate mybatis mapper xml and java file for 127.0.0.1/hmsbeta1 in ~/test2/ folder.

## License

Copyright © 2012 lysu

Distributed under the Eclipse Public License, the same as Clojure.