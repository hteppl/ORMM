package ru.leonidm.ormm.orm.queries.select;

import org.jetbrains.annotations.NotNull;
import ru.leonidm.ormm.orm.ORMColumn;
import ru.leonidm.ormm.orm.ORMTable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public final class RawSelectQuery<T> extends AbstractSelectQuery<RawSelectQuery<T>, T, List<List<Object>>, List<Object>> {

    public RawSelectQuery(@NotNull ORMTable<T> table) {
        super(table);
    }

    @Override
    @NotNull
    protected Supplier<List<List<Object>>> prepareSupplier() {
        return () -> {

            try (Statement statement = table.getDatabase().getConnection().createStatement();
                 ResultSet resultSet = statement.executeQuery(getSQLQuery())) {

                List<List<Object>> out = new ArrayList<>();
                JoinsHandler<T, List<Object>> joinsHandler = new JoinsHandler<>(table, joins);

                while (resultSet.next()) {
                    List<Object> objects = new ArrayList<>(columns.length);

                    for (int i = 0; i < columns.length; i++) {
                        ORMColumn<T, ?> column = Objects.requireNonNull(table.getColumn(columns[i]));
                        objects.add(column.toFieldObject(resultSet.getObject(i + 1)));
                    }

                    out.add(objects);

                    joinsHandler.save(resultSet, objects);
                }

                joinsHandler.apply();
                return out;
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        };
    }

    @NotNull
    public RawSingleSelectQuery<T> single() {
        RawSingleSelectQuery<T> rawSingleSelectQuery = new RawSingleSelectQuery<>(table);

        copy(rawSingleSelectQuery);
        rawSingleSelectQuery.limit = 1;

        return rawSingleSelectQuery;
    }
}
