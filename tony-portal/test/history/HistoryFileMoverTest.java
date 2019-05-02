package history;

//import com.google.common.io.Files;
import com.google.common.io.Files;
import com.linkedin.tony.Constants;
import com.linkedin.tony.TonyConfigurationKeys;
import com.linkedin.tony.util.ParserUtils;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import hadoop.Requirements;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class HistoryFileMoverTest {
  @Mock
  Config config;

  @Mock
  Requirements reqs;

  private static File tempDir;
  private static File intermediateDir;
  private static File finishedDir;

  @BeforeClass
  public static void setup() {
    tempDir = Files.createTempDir();
    intermediateDir = new File(tempDir, "intermediate");
    intermediateDir.mkdirs();
    finishedDir = new File(tempDir, "finished");
    finishedDir.mkdirs();
  }

  @Test
  public void testMoveIntermediateToFinished() throws IOException, InterruptedException {
    // Add a completed application in the intermediate dir
    String appId = "application_123_456";
    File appDir = new File(intermediateDir, appId);
    appDir.mkdirs();
    long endTime = System.currentTimeMillis();
    File events = new File(appDir, appId + "-123-" + endTime + "-user1-SUCCEEDED." + Constants.HISTFILE_SUFFIX);
    events.createNewFile();

    // Make sure year/month/day directories created in finished directory are based on finished time set in
    // jhist file name and NOT based off the application directory's modification time.
    if (!appDir.setLastModified(0)) {
      throw new RuntimeException();
    }

    when(config.hasPath(TonyConfigurationKeys.TONY_HISTORY_MOVER_INTERVAL_MS)).thenReturn(false);
    when(reqs.getFileSystem()).thenReturn(FileSystem.getLocal(new Configuration()));
    when(reqs.getIntermDir()).thenReturn(new Path(intermediateDir.getAbsolutePath()));
    when(reqs.getFinishedDir()).thenReturn(new Path(finishedDir.getAbsolutePath()));

    // start mover
    new HistoryFileMover(config, reqs);
    Thread.sleep(250);

    // verify application directory was moved
    Date endDate = new Date(endTime);
    File finalDir = new File(finishedDir, ParserUtils.getYearMonthDayDirectory(endDate) + Path.SEPARATOR + appId);
    Assert.assertFalse(appDir.exists());
    Assert.assertTrue(finalDir.isDirectory());
  }
}