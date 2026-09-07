package io.vidocq.mansart.persistence.core.jpql;

import org.junit.jupiter.api.Test;
import static io.vidocq.mansart.persistence.core.jpql.JpqlAst.*;
import static org.assertj.core.api.Assertions.*;

class JpqlParserTest {

    private final JpqlParser parser = new JpqlParser();

    @Test
    void parseSimpleSelect() {
        String query = "SELECT e FROM Employee e";
        JpqlStatement stmt = parser.parse(query);
        
        assertThat(stmt).isInstanceOf(SelectStatement.class);
        SelectStatement select = (SelectStatement) stmt;
        assertThat(select.select()).isNotNull();
        assertThat(select.from()).isNotNull();
        assertThat(select.where()).isNull();
        assertThat(select.groupBy()).isNull();
        assertThat(select.having()).isNull();
        assertThat(select.orderBy()).isNull();
    }

    @Test
    void parseSelectWithNamedParameter() {
        String query = "SELECT e FROM Employee e WHERE e.name = :name";
        JpqlStatement stmt = parser.parse(query);
        
        assertThat(stmt).isInstanceOf(SelectStatement.class);
        SelectStatement select = (SelectStatement) stmt;
        assertThat(select.where()).isNotNull();
        // Further assertions on AST structure would go here
    }

    @Test
    void parseSelectWithAndCondition() {
        String query = "SELECT e FROM Employee e WHERE e.salary > 50000 AND e.active = TRUE";
        JpqlStatement stmt = parser.parse(query);
        
        assertThat(stmt).isInstanceOf(SelectStatement.class);
    }

    @Test
    void parseSelectWithLike() {
        String query = "SELECT e FROM Employee e WHERE e.name LIKE 'J%' ESCAPE '\\'";
        JpqlStatement stmt = parser.parse(query);
        
        assertThat(stmt).isInstanceOf(SelectStatement.class);
    }

    @Test
    void parseSelectWithBetween() {
        String query = "SELECT e FROM Employee e WHERE e.age BETWEEN 25 AND 65";
        JpqlStatement stmt = parser.parse(query);
        
        assertThat(stmt).isInstanceOf(SelectStatement.class);
    }

    @Test
    void parseSelectWithInList() {
        String query = "SELECT e FROM Employee e WHERE e.dept IN ('A', 'B', 'C')";
        JpqlStatement stmt = parser.parse(query);
        
        assertThat(stmt).isInstanceOf(SelectStatement.class);
    }

    @Test
    void parseSelectWithDistinctAndOrderBy() {
        String query = "SELECT DISTINCT e.name FROM Employee e ORDER BY e.name ASC";
        JpqlStatement stmt = parser.parse(query);
        
        assertThat(stmt).isInstanceOf(SelectStatement.class);
    }

    @Test
    void parseSelectWithAggregate() {
        String query = "SELECT COUNT(e) FROM Employee e";
        JpqlStatement stmt = parser.parse(query);
        
        assertThat(stmt).isInstanceOf(SelectStatement.class);
    }

    @Test
    void parseUpdateStatement() {
        String query = "UPDATE Employee e SET e.salary = e.salary * 1.1 WHERE e.active = TRUE";
        JpqlStatement stmt = parser.parse(query);
        
        assertThat(stmt).isInstanceOf(UpdateStatement.class);
    }

    @Test
    void parseDeleteStatement() {
        String query = "DELETE FROM Employee e WHERE e.active = FALSE";
        JpqlStatement stmt = parser.parse(query);
        
        assertThat(stmt).isInstanceOf(DeleteStatement.class);
    }

    @Test
    void parseSelectWithJoin() {
        String query = "SELECT e FROM Employee e JOIN e.department d WHERE d.name = 'Eng'";
        JpqlStatement stmt = parser.parse(query);
        
        assertThat(stmt).isInstanceOf(SelectStatement.class);
    }

    @Test
    void parseInvalidSelectFromMissingIdentificationVariable() {
        String query = "SELECT FROM Employee";
        
        assertThatThrownBy(() -> parser.parse(query))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("position");
    }
}
