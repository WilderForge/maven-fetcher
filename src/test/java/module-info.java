module org.myjtools.mavenfetcher.test {
    requires org.myjtools.mavenfetcher;
    requires org.slf4j;
    requires org.assertj.core;
    requires org.junit.jupiter.api;

    opens org.myjtools.mavenfetcher.test to org.junit.platform.commons;
}