# how to run

* build bridge image `cd ../bridge; ./mvnw clean package jib:dockerBuild "-Djib.to.tags=local"`
* build auth0 sample image 
* `ngrok http 8080`
* copy public url and change the `SAMPLE_PUBLIC_URL` value in the `.env` file
* change `BASE_APP_URL` secret the auth0 action
* update tru.ID issuer settings and OIDC client