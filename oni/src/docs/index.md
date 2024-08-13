# oni

![MBARI logo](images/logo-mbari-3b.png)

Onis is a web service that provides a RESTful API for managing a tree structure of names used by annotation tools and data catalogs. We typically call this tree structure a "knowledgebase" (aka "KB"). Tree structures are useful for modeling organism phylogeny. At MBARI, we also include other terms, such as geological features and equipment used for research. 

It is a replacement for <https://github.com/mbari-org/vars-kb-server> and <https://github.com/mbari-org/vars-user-server>. 

Oni includes APIs for fast search and retrieval of terms, fetching branches of the knowledgebase, and user accounts. Individual nodes in the KB are called _concepts_, and each concept may have one primary name (e.g. the accepted taxa name) and zero or more alternative names (such as synonyms, common names, former taxa names, etc.)

[Scala API](api/index.html)

