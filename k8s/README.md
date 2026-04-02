# advweb k3s POC

Current server state:

- k3s is installed on the VPS
- namespace: advweb
- deployment: advweb-demo
- service: NodePort
- app NodePort: 30081
- keycloak NodePort: 30082
- backend runs on port 8088 inside the pod
- postgres data is mounted from /var/lib/docker/volumes/advweb_db_data/_data
- uploads are mounted from /var/lib/docker/volumes/advweb_uploads/_data

Traffic flow:

- / redirects to /swagger-ui/index.html
- /advertisements, /categories, /users, /comments, /reviews, /images, /avatars, /admin go to backend
- /swagger-ui and /v3/api-docs go to backend
- /uploads goes to backend file storage
- /auth goes to keycloak

Apply manifests:

```bash
kubectl apply -f k8s/advweb.yaml
```
