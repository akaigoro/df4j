There are control flow paradigm and dataflow paradigm.
java.util.concurrent exploits control flow.
This framework is an attempt to extend j.u.c to dataflow.
I am not going to recreate all features of j.u.c in dataflow manner.
Instead, only small number of basic blocks will be implemented.
However, this will be sufficient to make dataflow equivalent of such complex things like ScheduledThreadPoolExecutor .
The trick is to handle tasks themselves as dataflow tokens.