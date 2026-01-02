package acme.plugin

import nextflow.Session
import spock.lang.Specification

/**
 * Implements a basic factory test
 *
 */
class MyObserverTest extends Specification {

    def 'should create the observer instance' () {
        given:
        def factory = new MyFactory()
        when:
        def result = factory.create(Mock(Session))
        then:
        result.size() == 1
        result.first() instanceof MyObserver
    }

}
