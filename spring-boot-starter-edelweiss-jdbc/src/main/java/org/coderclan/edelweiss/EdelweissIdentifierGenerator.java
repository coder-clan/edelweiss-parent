//package org.coderclan.edelweiss;
//
//import org.coderclan.edelweiss.IdGenerator;
//import org.hibernate.HibernateException;
//import org.hibernate.engine.spi.SharedSessionContractImplementor;
//import org.hibernate.id.IdentifierGenerator;
//import org.springframework.beans.factory.annotation.Autowired;
//
//import java.io.Serializable;
//
///**
// * @author aray(dot)chou(dot)cn(at)gmail(dot)com
// */
//public class EdelweissIdentifierGenerator implements IdentifierGenerator {
//    @Autowired
//    private IdGenerator idGenerator;
//
//    @Override
//    public Serializable generate(SharedSessionContractImplementor session, Object object) throws HibernateException {
//        return idGenerator.generateId();
//    }
//
//
//}
