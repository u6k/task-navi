
package me.u6k.task_focus.service;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import me.u6k.task_focus.model.Task;
import me.u6k.task_focus.model.TaskRepository;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TaskServiceTest {

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskRepository taskRepo;

    @Before
    public void setup() {
        this.taskRepo.deleteAllInBatch();
    }

    @Test
    public void create() {
        List<Task> l = this.taskRepo.findAll();

        assertThat(l.size(), is(0));

        this.taskService.create(new Date(), "テスト作業1", 60, null);
        this.taskService.create(new Date(), "テスト作業2", 10, null);
        this.taskService.create(new Date(), "テスト作業3", 0, null);

        l = this.taskRepo.findAll();

        assertThat(l.size(), is(3));
    }

    @Test
    public void create_作業日が空の場合はエラー() {
        try {
            this.taskService.create(null, "テスト作業", 0, null);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("date is null."));
        }
    }

    @Test
    public void create_作業名が空の場合はエラー() throws Exception {
        Date date = new SimpleDateFormat("yyyy-MM-dd").parse("2015-12-23");

        // nullはエラー
        try {
            this.taskService.create(date, null, 0, null);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("name is blank."));
        }

        // 空文字列はエラー
        try {
            this.taskService.create(date, "", 0, null);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("name is blank."));
        }

        // 空白のみはエラー
        try {
            this.taskService.create(date, "   ", 0, null);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("name is blank."));
        }

        // 前後の空白はトリミング
        this.taskService.create(date, "  テスト作業   ", 0, null);

        List<Task> l = this.taskRepo.findAll();
        assertThat(l.size(), is(1));

        Task t = l.get(0);
        assertNotNull(t.getId());
        assertTrue(DateUtils.isSameInstant(t.getDate(), date));
        assertThat(t.getOrderOfDate(), is(0));
        assertThat(t.getName(), is("テスト作業"));
        assertThat(t.getEstimatedTime(), is(0));
        assertNull(t.getEstimatedStartTime());
        assertNull(t.getStartTime());
        assertNull(t.getEndTime());
    }

    @Test
    public void create_見積り時間がマイナスの場合はエラー() throws Exception {
        Date date = new SimpleDateFormat("yyyy-MM-dd").parse("2015-12-23");

        // マイナスはエラー
        try {
            this.taskService.create(date, "テスト作業", -1, null);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("estimatedTime < 0. estimatedTime=-1"));
        }

        // マイナスはエラー
        try {
            this.taskService.create(date, "テスト作業", -123, null);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("estimatedTime < 0. estimatedTime=-123"));
        }

        // 0は正常
        UUID id = this.taskService.create(date, "テスト作業", 0, null);

        Task t = this.taskRepo.findOne(id);
        assertThat(t.getId(), is(id));
        assertTrue(DateUtils.isSameInstant(t.getDate(), date));
        assertThat(t.getOrderOfDate(), is(0));
        assertThat(t.getName(), is("テスト作業"));
        assertThat(t.getEstimatedTime(), is(0));
        assertNull(t.getEstimatedStartTime());
        assertNull(t.getStartTime());
        assertNull(t.getEndTime());

        // プラスは正常
        id = this.taskService.create(date, "テスト作業", 1, null);

        t = this.taskRepo.findOne(id);
        assertThat(t.getId(), is(id));
        assertTrue(DateUtils.isSameInstant(t.getDate(), date));
        assertThat(t.getOrderOfDate(), is(0));
        assertThat(t.getName(), is("テスト作業"));
        assertThat(t.getEstimatedTime(), is(1));
        assertNull(t.getEstimatedStartTime());
        assertNull(t.getStartTime());
        assertNull(t.getEndTime());

        // プラスは正常
        id = this.taskService.create(date, "テスト作業", 234, null);

        t = this.taskRepo.findOne(id);
        assertThat(t.getId(), is(id));
        assertTrue(DateUtils.isSameInstant(t.getDate(), date));
        assertThat(t.getOrderOfDate(), is(0));
        assertThat(t.getName(), is("テスト作業"));
        assertThat(t.getEstimatedTime(), is(234));
        assertNull(t.getEstimatedStartTime());
        assertNull(t.getStartTime());
        assertNull(t.getEndTime());
    }

    @Test
    public void create_開始予定時刻と作業日が異なる場合はエラー() throws Exception {
        Date date = new SimpleDateFormat("yyyy-MM-dd").parse("2015-12-23");

        try {
            Date estimatedStartTime = new SimpleDateFormat("yyyy-MM-dd HH:mm").parse("2015-12-24 13:00");
            this.taskService.create(date, "テスト作業", 0, estimatedStartTime);

            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("date and estimatedStartTime are different day. date=Wed Dec 23 00:00:00 JST 2015, estimatedStartTime=Thu Dec 24 13:00:00 JST 2015"));
        }

        Date estimatedStartTime = new SimpleDateFormat("yyyy-MM-dd HH:mm").parse("2015-12-23 14:27");
        UUID id = this.taskService.create(date, "テスト作業", 0, estimatedStartTime);
        Task t = this.taskRepo.findOne(id);
        assertThat(t.getId(), is(id));
        assertTrue(DateUtils.isSameInstant(t.getDate(), date));
        assertThat(t.getOrderOfDate(), is(0));
        assertThat(t.getName(), is("テスト作業"));
        assertThat(t.getEstimatedTime(), is(0));
        assertTrue(DateUtils.isSameInstant(t.getEstimatedStartTime(), estimatedStartTime));
        assertNull(t.getStartTime());
        assertNull(t.getEndTime());
    }

}