package org.df4j.pipeline.core;

import java.util.ArrayList;
import org.df4j.pipeline.df4j.core.CompletableFuture;
import org.df4j.pipeline.df4j.core.DataflowVariable;

/**
 * PipeLine p=new PipeLine();
 * p.setSource(new MySource())
 *  .addTransformer(new MyTransformer1())
 *  .addTransformer(new MyTransformer2())
 *  .setSink(new MySink());
 *  
 *  p.start();
 *  p.get(); // wait pipeline to finish; can rethrow errors. 
 * 
 * @author Alexei Kaigorodov
 *
 */
public class Pipeline extends CompletableFuture<Object> {
    Source<?> source;
    Sink<?> sink;
    ArrayList<Bolt> nodes=new ArrayList<Bolt>();
    
    @Override
    public void post(Object m) {
        try {
            super.post(m);
            stop();
        } catch (IllegalStateException e) {
        }
    }

    @Override
    public void postFailure(Throwable newExc) {
        try {
            super.postFailure(newExc);
            stop();
        } catch (IllegalStateException e) {
        }
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (super.cancel(mayInterruptIfRunning)) {
            stop();
            return true;
        }
        return false;
    }

    public Source<?> getSource() {
        return source;
    }
    
    public Sink<?> getSink() {
        return sink;
    }
    
    public Pipeline start() {
        for (Bolt node: nodes) {
            node.start();
        }
        return this;
    }

    public void stop() {
        for (Bolt node: nodes) {
            node.stop();
        }
    }

    public <O> Connector<O> setSource(Source<O> newSource) {
        if (source==null) {
            source=newSource; // only first source set
        } else if (sink==null) {
            throw new IllegalStateException("previous chain not finished");
        } else {
            sink=null;
        }
        nodes.add(newSource);
        newSource.setContext(this);
        return new Connector<O>(newSource);
    }

    public <T> void connect(Source<T> source, Sink<T> sink) {
        nodes.add(sink);
        sink.setContext(this);
        source.setSinkPort(sink.getInputPort());
        sink.setReturnPort(source.getReturnPort());
    }

    public class Connector<I> {
        protected Source<I> source;
        
        public Connector(Source<I> source) {
            this.source = source;
        }
        
        public <O> Connector<O> addTransformer(Transformer<I,O> tr) {
            if (source==null) {
                throw new IllegalStateException("chain not started");
            }
            if (sink!=null) {
                throw new IllegalStateException("chain finished");
            }
            Pipeline.this.connect(source, tr);
            return new Connector<O>(tr);
        }
        
        public Pipeline setSink(Sink<I> newSink) {
            if (source==null) {
                throw new IllegalStateException("chain not started");
            }
            if (sink!=null) {
                throw new IllegalStateException("chain already finished");
            }
            sink=newSink;
            connect(source, newSink);
            return Pipeline.this;
        }
    }

    public String getStatus() {
        StringBuilder sb=new StringBuilder();
        for (Bolt node: nodes) {
            if (node instanceof DataflowVariable) {
                DataflowVariable dfv=(DataflowVariable)node;
                String status=dfv.getStatus();
                sb.append(dfv.getClass().getSimpleName());
                sb.append(": ");
                sb.append(status);
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    public void close() {
        // TODO Auto-generated method stub
        
    }
}
// TODO reaction to multiple postFailures and postFailure after post(value)