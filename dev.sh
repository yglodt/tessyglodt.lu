#!/bin/sh
# Local development in Docker: Postgres 18 and the app with hot reload on http://localhost:8017
#
#   ./dev.sh              start the DB and the app (Ctrl-C stops the app, the DB keeps running)
#   ./dev.sh db           start only the DB (for tools/ on the host, or the app run from VS Code)
#   ./dev.sh import FILE  replace the database with a pg_dump (custom format .dump, or plain .sql)
#   ./dev.sh psql         psql in the database
#   ./dev.sh stop         stop everything (the data stays in the "db" volume)
#
# Inside the app container (compose.yaml) it runs as "./dev.sh container": the app with devtools,
# plus a loop that recompiles when a Java file or the pom changes, after which devtools restarts
# the app. Templates and static files need no rebuild: the container reads them from src/.
set -e
cd "$(dirname "$0")"

case "${1:-up}" in
up)
	docker compose up -d --wait db
	exec docker compose up app
	;;
db)
	docker compose up -d --wait db
	;;
import)
	[ -f "$2" ] || { echo "Usage: $0 import FILE"; exit 1; }
	docker compose stop app
	docker compose up -d --wait db
	docker compose exec -T db dropdb -U tessyglodt --if-exists --force tessyglodt
	docker compose exec -T db createdb -U tessyglodt -O tessyglodt tessyglodt
	case "$2" in
	*.sql) docker compose exec -T db psql -q -v ON_ERROR_STOP=1 -U tessyglodt -d tessyglodt < "$2" ;;
	*) docker compose exec -T db pg_restore --no-owner --no-privileges -U tessyglodt -d tessyglodt < "$2" ;;
	esac
	echo "Imported $2"
	;;
psql)
	shift
	exec docker compose exec db psql -U tessyglodt -d tessyglodt "$@"
	;;
stop)
	docker compose stop
	;;
container)
	mvn -q compile
	touch target/.compiled
	(
		while sleep 2; do
			if [ -n "$(find src/main/java pom.xml -newer target/.compiled -type f -print -quit)" ]; then
				touch target/.compiled
				echo "Recompiling..."
				# fork: in-process javac on JDK 25 can crash instead of reporting errors (see CLAUDE.md)
				mvn -q -o compile -Dmaven.compiler.fork=true || echo "Compile failed, the app keeps the previous classes"
			fi
		done
	) &
	# debugger port 5017: "Attach to app in Docker" in .vscode/launch.json
	exec mvn spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5017"
	;;
*)
	sed -n '2,8p' "$0"
	exit 1
	;;
esac
