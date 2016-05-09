package dataMgmt;

import org.junit.Assert;
import org.junit.Test;

import java.util.Observer;

/**
 * Created by fahslaj on 5/9/2016.
 */
public class TestSessionStorage {
    @Test
    public void testChangeAndRemoveProjectUserStatusNotify() {
        SessionStorage storage = DataManager.getInstance().getSessionStorage();
        final int[] access = {0};
        Observer obs = (o, arg) -> access[0]++;
        storage.addObserver(obs);
        storage.changeProjectUserStatus("hello", OnlineStatus.CONNECTED);
        Assert.assertEquals(1, access[0]);
        storage.removeProjectUserStatus("hello");
        Assert.assertEquals(2, access[0]);
    }

    @Test
    public void testAuthToken() {
        SessionStorage storage = DataManager.getInstance().getSessionStorage();
        Assert.assertEquals(null, storage.getAuthenticationToken());
        storage.setAuthenticationToken("Hello");
        Assert.assertEquals("Hello", storage.getAuthenticationToken());
    }
}
