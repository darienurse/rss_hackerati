
package com.appleappstorestop25.app.ItunesItemClasses;

import com.google.gson.annotations.Expose;

import javax.annotation.Generated;
import javax.validation.Valid;
import java.io.Serializable;

@Generated("org.jsonschema2pojo")
public class ItunesRSSResponse implements Serializable {

    @Expose
    @Valid
    private Feed feed;

    /**
     * 
     * @return
     *     The feed
     */
    public Feed getFeed() {
        return feed;
    }

    /**
     * 
     * @param feed
     *     The feed
     */
    public void setFeed(Feed feed) {
        this.feed = feed;
    }

    public ItunesRSSResponse withFeed(Feed feed) {
        this.feed = feed;
        return this;
    }

}
