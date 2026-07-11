"""RabbitMQ topology and exchange/queue names."""
from enum import Enum


class Exchange(str, Enum):
    DEFAULT = "myblog.events"


class Queue(str, Enum):
    VISIT_RECORD = "myblog.visit.record"
    EMAIL_NOTIFY = "myblog.email.notify"
    FILE_CLEANUP = "myblog.file.cleanup"
    AUDIT_LOG = "myblog.audit.log"


class RoutingKey(str, Enum):
    VISIT_RECORD = "visit.record"
    EMAIL_NOTIFY = "email.notify"
    FILE_CLEANUP = "file.cleanup"
    AUDIT_LOG = "audit.log"


# Mapping used when binding queues
QUEUE_BINDINGS = {
    Queue.VISIT_RECORD.value: RoutingKey.VISIT_RECORD.value,
    Queue.EMAIL_NOTIFY.value: RoutingKey.EMAIL_NOTIFY.value,
    Queue.FILE_CLEANUP.value: RoutingKey.FILE_CLEANUP.value,
    Queue.AUDIT_LOG.value: RoutingKey.AUDIT_LOG.value,
}