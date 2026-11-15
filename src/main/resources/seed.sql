INSERT INTO professions (id, code, name) VALUES
    (1, 'java-dev', 'Java Developer'),
    (2, 'python-dev', 'Python Developer')
ON CONFLICT (id) DO NOTHING;

INSERT INTO skills (id, name, category, canonical_name) VALUES
    (1, 'Java', 'Language', 'Java'),
    (2, 'SQL', 'Database', 'SQL'),
    (3, 'Spring', 'Framework', 'Spring'),
    (4, 'Docker', 'DevOps', 'Docker'),
    (5, 'Git', 'Tools', 'Git'),
    (6, 'PostgreSQL', 'Database', 'PostgreSQL')
ON CONFLICT (id) DO NOTHING;

INSERT INTO skill_synonyms (skill_id, synonym) VALUES
    (1, 'java'), (1, 'джава'),
    (2, 'sql'), (2, 'базы данных'),
    (3, 'spring'), (3, 'spring boot'), (3, 'спринг'),
    (4, 'docker'), (4, 'докер'),
    (5, 'git'), (5, 'гит'),
    (6, 'postgresql'), (6, 'postgres'), (6, 'постгрес'), (6, 'базы данных')
ON CONFLICT (skill_id, synonym) DO NOTHING;

INSERT INTO profession_profiles (id, profession_id, grade, sample_size, low_data) VALUES
    (1, 1, 'MIDDLE', 120, FALSE)
ON CONFLICT (id) DO NOTHING;

INSERT INTO profession_profile_skills (profile_id, skill_id, weight, required_level) VALUES
    (1, 1, 0.95, 4),
    (1, 3, 0.85, 4),
    (1, 2, 0.70, 3),
    (1, 4, 0.40, 3)
ON CONFLICT DO NOTHING;

INSERT INTO test_questions (skill_id, level, question, options, correct_index, explanation, topic, source) VALUES
(1, 3, 'Какой интерфейс реализует HashMap?', '["List","Map","Set","Queue"]', 1, 'HashMap реализует интерфейс java.util.Map.', 'Collections', 'bank'),
(1, 3, 'Что делает ключевое слово final для переменной?', '["Делает её потокобезопасной","Запрещает переприсваивание","Выделяет в куче","Делает статической"]', 1, 'final запрещает изменение ссылки или примитивного значения после инициализации.', 'OOP', 'bank'),
(1, 3, 'Какое исключение бросается при делении int на 0?', '["NullPointerException","ArithmeticException","IOException","NumberFormatException"]', 1, 'Целочисленное деление на ноль вызывает ArithmeticException.', 'Exceptions', 'bank'),
(1, 4, 'Что гарантирует ключевое слово volatile в Java?', '["Атомарность i++","Видимость изменений между потоками","Блокировку монитора","Отсутствие deadlock"]', 1, 'volatile обеспечивает happens-before и чтение актуального значения из основной памяти.', 'Concurrency', 'bank'),
(1, 4, 'Какая сложность поиска по ключу в HashMap в среднем случае?', '["O(1)","O(log n)","O(n)","O(n log n)"]', 0, 'При хорошем распределении hashCode доступ занимает O(1).', 'Collections', 'bank'),
(1, 4, 'Что происходит при коллизии в HashMap в Java 8+ при >8 элементах в бакете?', '["Выбрасывается ошибка","Список преобразуется в красно-чёрное дерево","Таблица очищается","Создаётся новый поток"]', 1, 'При достижении TREEIFY_THRESHOLD (8) связный список перестраивается в TreeNode.', 'Collections', 'bank'),
(1, 5, 'Какой сборщик мусора используется по умолчанию в Java 21?', '["Serial GC","Parallel GC","G1 GC","ZGC"]', 2, 'G1 GC является сборщиком по умолчанию начиная с Java 9.', 'JVM', 'bank'),
(1, 5, 'Что такое false sharing в многопоточности?', '["Утечка памяти","Конкуренция потоков за одну кэш-линию процессора","Ошибка сериализации","Взаимная блокировка"]', 1, 'Изменение разных переменных в одной кэш-линии инвалидирует кэш других ядер.', 'Concurrency', 'bank'),
(1, 5, 'Какую проблему решают Virtual Threads (Project Loom)?', '["Ускоряют вычисления CPU","Дешевое масштабирование блокирующих I/O операций","Заменяют GC","Убирают synchronized"]', 1, 'Виртуальные потоки не блокируют потоки ОС при ожидании I/O.', 'Concurrency', 'bank');

INSERT INTO test_questions (skill_id, level, question, options, correct_index, explanation, topic, source) VALUES
(3, 3, 'Какая область видимости (Scope) бина в Spring по умолчанию?', '["prototype","singleton","request","session"]', 1, 'По умолчанию Spring создаёт один экземпляр бина на контейнер (singleton).', 'IoC', 'bank'),
(3, 3, 'Какая аннотация используется для внедрения зависимости?', '["@Autowired","@Entity","@Table","@RequestMapping"]', 0, '@Autowired указывает контейнеру внедрить зависимый бин.', 'DI', 'bank'),
(3, 3, 'Что делает @RestController?', '["Создаёт таблицу в БД","Объединяет @Controller и @ResponseBody","Настраивает Kafka","Запускает транзакцию"]', 1, '@RestController сериализует возвращаемые объекты напрямую в тело HTTP-ответа.', 'Web', 'bank'),
(3, 4, 'Почему @Transactional не сработает при вызове метода внутри того же класса?', '["Нужен public static","Вызов идёт мимо Spring Proxy (self-invocation)","Не указан isolation","Нужен @Async"]', 1, 'Spring AOP оборачивает бин в прокси; внутренний вызов через this обходит прокси.', 'Транзакции', 'bank'),
(3, 4, 'При каких исключениях @Transactional откатывает транзакцию по умолчанию?', '["При любых Throwable","Только при RuntimeException и Error","Только при SQLException","Никогда"]', 1, 'По умолчанию откат происходит только на непроверяемых исключениях (unchecked).', 'Транзакции', 'bank'),
(3, 4, 'Как работает Spring AOP по умолчанию для классов с интерфейсами?', '["Рефлексия полей","Динамические прокси (JDK / CGLIB)","Изменение байткода при компиляции","Не работает"]', 1, 'Spring создаёт runtime-прокси через CGLIB или JDK Dynamic Proxy.', 'AOP', 'bank'),
(3, 5, 'Какой уровень изоляции предотвращает фантомное чтение в стандарте SQL/Spring?', '["READ_UNCOMMITTED","READ_COMMITTED","REPEATABLE_READ","SERIALIZABLE"]', 3, 'SERIALIZABLE полностью изолирует транзакции от фантомных записей.', 'Транзакции', 'bank'),
(3, 5, 'Что делает BeanPostProcessor?', '["Удаляет неиспользуемые таблицы","Позволяет модифицировать бины до и после инициализации","Парсит YAML","Управляет пулом соединений"]', 1, 'BeanPostProcessor перехватывает создание бинов (на нём работают @Autowired и прокси).', 'IoC', 'bank'),
(3, 5, 'Что делает propagation = Propagation.REQUIRES_NEW?', '["Использует текущую транзакцию","Приостанавливает текущую и открывает новую независимую транзакцию","Запрещает транзакцию","Читает только из кэша"]', 1, 'REQUIRES_NEW всегда стартует отдельную физическую транзакцию.', 'Транзакции', 'bank');

INSERT INTO test_questions (skill_id, level, question, options, correct_index, explanation, topic, source) VALUES
(2, 2, 'Какой оператор фильтрует строки до группировки GROUP BY?', '["HAVING","WHERE","ORDER BY","LIMIT"]', 1, 'WHERE фильтрует исходные строки, HAVING — уже сгруппированные.', 'Queries', 'bank'),
(2, 2, 'Какой JOIN вернёт все строки из левой таблицы даже без совпадений справа?', '["INNER JOIN","LEFT JOIN","RIGHT JOIN","CROSS JOIN"]', 1, 'LEFT JOIN сохраняет все записи левой таблицы, заполняя правые поля NULL.', 'Joins', 'bank'),
(2, 2, 'Как удалить дубликаты из результата SELECT?', '["UNIQUE","DISTINCT","GROUP","FILTER"]', 1, 'SELECT DISTINCT возвращает только уникальные кортежи.', 'Queries', 'bank'),
(2, 3, 'Для чего нужен внешний ключ (FOREIGN KEY)?', '["Ускоряет сортировку","Обеспечивает ссылочную целостность между таблицами","Шифрует столбец","Создаёт копию таблицы"]', 1, 'FOREIGN KEY гарантирует, что значение существует в родительской таблице.', 'Constraints', 'bank'),
(2, 3, 'Что вернёт COUNT(column_name), если в столбце есть NULL?', '["Посчитает все строки включая NULL","Посчитает только не-NULL значения","Вернёт NULL","Выдаст ошибку"]', 1, 'COUNT(col) игнорирует NULL, в отличие от COUNT(*).', 'Aggregates', 'bank'),
(2, 3, 'Какая команда полностью удаляет таблицу и её структуру?', '["DELETE TABLE","TRUNCATE TABLE","DROP TABLE","REMOVE TABLE"]', 2, 'DROP TABLE удаляет саму таблицу из схемы БД.', 'DDL', 'bank'),
(2, 4, 'Когда B-Tree индекс НЕ используется при поиске по строковому полю?', '["WHERE name = ''Ivan''","WHERE name LIKE ''Ivan%''","WHERE name LIKE ''%Ivan''","WHERE name IN (''A'',''B'')"]', 2, 'Ведущий wildcard ''%...'' не позволяет идти по дереву индекса слева направо.', 'Indexes', 'bank'),
(2, 4, 'Что делает команда EXPLAIN ANALYZE в PostgreSQL?', '["Создаёт индекс","Реально выполняет запрос и показывает план с фактическим временем","Очищает таблицу","Проверяет синтаксис без запуска"]', 1, 'EXPLAIN ANALYZE запускает запрос и выводит реальные тайминги и число строк.', 'Performance', 'bank'),
(2, 4, 'Какое свойство ACID гарантирует сохранение данных после COMMIT даже при сбое питания?', '["Atomicity","Consistency","Isolation","Durability"]', 3, 'Durability (долговечность) обеспечивается записью в WAL-журнал.', 'Transactions', 'bank');

SELECT setval(pg_get_serial_sequence('professions', 'id'), COALESCE(MAX(id), 1)) FROM professions;
SELECT setval(pg_get_serial_sequence('skills', 'id'), COALESCE(MAX(id), 1)) FROM skills;
SELECT setval(pg_get_serial_sequence('skill_synonyms', 'id'), COALESCE(MAX(id), 1)) FROM skill_synonyms;
SELECT setval(pg_get_serial_sequence('profession_profiles', 'id'), COALESCE(MAX(id), 1)) FROM profession_profiles;
SELECT setval(pg_get_serial_sequence('test_questions', 'id'), COALESCE(MAX(id), 1)) FROM test_questions;
