package clientcore.dataMgmt;

import org.junit.Assert;
import org.junit.Test;
import clientcore.websocket.models.Project;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;

/**
 * Created by fahslaj on 5/9/2016.
 */
public class TestSessionStorage {
    @Test
    public void testChangeAndRemoveProjectUserStatusNotify() {
        SessionStorage storage = DataManager.getInstance().getSessionStorage();
        final int[] access = {0};
        PropertyChangeListener listener = (propChangeEvent) -> {
            if (propChangeEvent.getPropertyName().equals(SessionStorage.PROJECT_USER_STATUS)) {
                access[0]++;
            }
        };
        storage.addPropertyChangeListener(listener);
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

    @Test
    public void testUsernameAndPropertyListener() {
        SessionStorage storage = DataManager.getInstance().getSessionStorage();
        final int[] access = {0};
        PropertyChangeListener listener = (event) -> {
            if (event.getPropertyName().equals(SessionStorage.USERNAME)) {
                access[0]++;
            }
        };
        storage.addPropertyChangeListener(listener);
        Assert.assertEquals(null, storage.getUsername());
        storage.setUsername("austin");
        Assert.assertEquals("austin", storage.getUsername());
        Assert.assertEquals(1, access[0]);
    }

    @Test
    public void testProjectList() {
        SessionStorage storage = DataManager.getInstance().getSessionStorage();
        final int[] access = {0};
        PropertyChangeListener listener = (event) -> {
            if (event.getPropertyName().equals(SessionStorage.PROJECT_LIST)) {
                access[0]++;
            }
        };
        storage.addPropertyChangeListener(listener);
        Assert.assertEquals(0, storage.getProjects().size());
        Project project = new Project(0, "hi", null);
        storage.setProject(project);
        Assert.assertEquals(1, access[0]);
        Assert.assertEquals(project, storage.getProject(0));
        storage.setProjects(new ArrayList<>());
        Assert.assertEquals(2, access[0]);
    }
    
}
