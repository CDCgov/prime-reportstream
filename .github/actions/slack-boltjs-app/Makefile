default: start

start: build
	docker stop slack-boltjs-app || true
	docker rm slack-boltjs-app || true
	docker run --name=slack-boltjs-app --env-file .env \
		--volume gh_locks:/usr/app/src/.locks -d slack_boltjs_app

build:
	docker build -t slack_boltjs_app -f Dockerfile.example .