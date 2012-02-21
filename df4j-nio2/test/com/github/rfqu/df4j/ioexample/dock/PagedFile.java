package com.github.rfqu.df4j.ioexample.dock;

import java.io.File;

public class PagedFile {
    File file;
    PageCache cache=new PageCache(this,10);
    
    public PagedFile(File file) {
        this.file = file;
    }

    
}
