<databaseChangeLog
        xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.3.xsd">

    <changeSet id="{year}_{month}_{day}_{name}[-s]_00000000001" author="Ibrahima">
        <createTable tableName="{name}[-s]">
            <column name="id" type="bigint" autoIncrement="true">
                <constraints primaryKey="true" nullable="false"/>
            </column>

            <!--            add columns here-->

            <column name="created_at" type="TIMESTAMP">
                <constraints nullable="false"/>
            </column>
            <column name="updated_at" type="TIMESTAMP">
                <constraints nullable="false"/>
            </column>
            <column name="created_by" type="BIGINT"/>
            <column name="updated_by" type="BIGINT"/>
        </createTable>
    </changeSet>
</databaseChangeLog>