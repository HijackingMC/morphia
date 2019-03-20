package dev.morphia.mapping.experimental;

import com.mongodb.DBObject;
import com.mongodb.DBRef;
import com.mongodb.client.MongoCursor;
import dev.morphia.AdvancedDatastore;
import dev.morphia.Datastore;
import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.Mapper;
import dev.morphia.query.Query;

/**
 * @morphia.internal
 * @param <T>
 */
@SuppressWarnings("deprecation")
public class SingleReference<T> extends MorphiaReference<T> {
    private String collection;
    private Object id;
    private T value;

    /**
     * @morphia.internal
     */
    public SingleReference(final Datastore datastore, final MappedClass mappedClass, final String collection, final Object id) {
        super(datastore, mappedClass);
        this.collection = collection;
        this.id = id;
    }

    protected SingleReference(final T value) {
        set(value);
    }

    /**
     * @morphia.internal
     */
    protected String getCollection() {
        return collection;
    }

    /**
     * @morphia.internal
     */
    protected void setCollection(final String collection) {
        this.collection = collection;
    }


    @SuppressWarnings("unchecked")
    public T get() {
        if (value == null && id != null) {
            final MongoCursor<?> mongoCursor = buildQuery()
                                                   .filter("_id", id)
                                                   .find();
            value = (T) mongoCursor.tryNext();
        }
        return value;
    }

    public void set(T value) {
        if(getDatastore() != null) {
            id = getDatastore().getMapper().getId(value);
        }
        this.value = value;
    }

    protected Query<?> buildQuery() {
        final Query<?> query;
        if (collection == null) {
            query = getDatastore().find(getMappedClass().getClazz());
        } else {
            query = ((AdvancedDatastore) getDatastore()).find(getCollection(), getMappedClass().getClazz());
        }
        return query;
    }

    public Object getId() {
        return id;
    }

    public boolean isResolved() {
        return value != null;
    }

    @Override
    public Object encode(Mapper mapper, final Object value, final MappedField optionalExtraInfo) {
        if(isResolved()) {
            return wrapId(mapper, optionalExtraInfo, get());
        } else {
            return null;
        }

    }

    public static MorphiaReference<?> decode(final Datastore datastore,
                                             final Mapper mapper,
                                             final MappedField mappedField,
                                             final Class paramType, final DBObject dbObject) {
        final MappedClass mappedClass = mapper.getMappedClass(paramType);
        Object id = dbObject.get(mappedField.getMappedFieldName());
        String collection = null;
        if (id instanceof DBRef) {
            collection = ((DBRef) id).getCollectionName();
            final Class<?> klass = mapper.getClassFromCollection(((DBRef) id).getCollectionName());
            final MappedField idField = mapper.getMappedClass(klass).getMappedIdField();
            id = mapper.getConverters().decode(idField.getConcreteType(), ((DBRef) id).getId(), mappedField);
        }

        return new SingleReference(datastore, mappedClass, collection, id);
    }

}
