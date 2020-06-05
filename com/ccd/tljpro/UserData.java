package com.ccd.tljpro;

/**
 *
 * @author ccheng
 */
public interface UserData {
    public String getName();

    public String getId();

    public String getImage();

    public void fetchData(String token, Runnable callback);
}
