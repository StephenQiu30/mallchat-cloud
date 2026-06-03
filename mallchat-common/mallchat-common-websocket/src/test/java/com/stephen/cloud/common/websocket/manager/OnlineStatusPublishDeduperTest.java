package com.stephen.cloud.common.websocket.manager;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class OnlineStatusPublishDeduperTest {

    @Test
    void shouldRejectDuplicateDedupeIdWithinTtl() {
        OnlineStatusPublishDeduper deduper = new OnlineStatusPublishDeduper(60_000L);

        Assertions.assertTrue(deduper.tryAcquire("online_status:1:1"));
        Assertions.assertFalse(deduper.tryAcquire("online_status:1:1"));
        Assertions.assertTrue(deduper.tryAcquire("online_status:1:0"));
    }

    @Test
    void shouldAllowDuplicateWhenTtlDisabled() {
        OnlineStatusPublishDeduper deduper = new OnlineStatusPublishDeduper(0L);

        Assertions.assertTrue(deduper.tryAcquire("online_status:1:1"));
        Assertions.assertTrue(deduper.tryAcquire("online_status:1:1"));
    }
}
