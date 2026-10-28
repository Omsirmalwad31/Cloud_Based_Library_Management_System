# Library Management System — Docker, Terraform, Prometheus and Grafana

This is an infrastructure overlay for the existing repository, which uses React/Vite, Spring Boot, PostgreSQL, and Spring Boot Actuator. It replaces the empty Docker/Compose/Terraform/monitoring placeholders and adds Grafana provisioning files.

## What is included

- Dockerfiles for the Spring Boot backend (Java 25 LTS) and React/Vite frontend
- Docker Compose services for frontend, backend, PostgreSQL, Prometheus and Grafana
- Prometheus scrape configuration for `/actuator/prometheus`
- Grafana Prometheus datasource provisioning and an initial JVM/HTTP dashboard
- Terraform starter for an AWS EC2 Docker host, security group, IAM instance profile, ECR repository, private S3 bucket, and optional RDS (`create_rds=false` by default)
- A small Spring Security change allowing Prometheus to scrape the metrics endpoint. In Compose, the backend does not publish port 8081 to the host; Prometheus reaches it over the internal Docker network.

## A. Install/run locally first (recommended)

Requirements: Docker Desktop with Docker Compose v2, Git, and enough RAM for Java + PostgreSQL + monitoring containers.

1. Copy these files/folders into the root of your cloned project, preserving paths. Allow them to replace the corresponding empty placeholder files. Merge the included `backend/src/main/java/com/library/config/SecurityConfig.java` into the project as the same file (it is the existing security configuration with `/actuator/prometheus` added to the permitted matchers).
2. In PowerShell at the project root:

   ```powershell
   Copy-Item .env.example .env
   notepad .env
   ```

   Set `DB_PASSWORD` and `GRAFANA_PASSWORD` to your own local passwords. Keep `.env` out of Git; commit `.env.example`, not `.env`.

3. Build and start the stack:

   ```powershell
   docker compose config
   docker compose up --build -d
   docker compose ps
   ```

4. Open these local URLs:

   - Library web UI: http://localhost:8080
   - Prometheus: http://localhost:9090
   - Prometheus targets: http://localhost:9090/targets
   - Grafana: http://localhost:3000 (user `admin`, password from `.env`)

   Grafana should provision a folder called **Library System** and dashboard called **Library Management - Spring Boot**. Generate some UI/API traffic to populate request metrics. In Prometheus, query `up{job="library-backend"}`; value `1` indicates Prometheus can scrape the backend.

5. Inspect problems/logs:

   ```powershell
   docker compose logs --tail=150 backend
   docker compose logs --tail=100 prometheus
   docker compose logs --tail=100 grafana
   ```

6. Stop the services, keeping data volumes:

   ```powershell
   docker compose down
   ```

   To remove the local database/metrics/dashboard volumes too (destructive):

   ```powershell
   docker compose down -v
   ```

## AWS CI/CD credentials

The publish job supports GitHub OIDC and repository-secret credentials. OIDC is recommended and requires no AWS access keys in GitHub:

1. Create an IAM role whose trust policy trusts `token.actions.githubusercontent.com` for this repository.
2. Restrict the subject claim to `repo:Omsirmalwad31/Cloud_Based_Library_Management_System:ref:refs/heads/main` (or use the repository subject `repo:Omsirmalwad31/Cloud_Based_Library_Management_System:*`).
3. Grant the role permission for `sts:GetCallerIdentity`, ECR repository creation and push (`ecr:*` can be narrowed to the two repository ARNs), and read access to the workflow resources.
4. Add the role ARN as the GitHub Actions secret `AWS_ROLE_ARN`.

As a compatibility fallback, add both `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY`. Do not store both static credentials if OIDC is configured. The workflow derives the account ECR registry automatically and creates `library-backend` and `library-frontend` repositories when they do not exist.

The workflow_dispatch entry lets the pipeline be tested from another branch. It still deploys only after AWS credentials and the deployment secrets below are configured:

- `EC2_SSH_KEY`, `EC2_HOST`, `EC2_USER`
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`

Never commit AWS credentials or private deployment keys.

## B. Terraform — inspect before creating AWS resources

Terraform manages cloud resources, not the application code itself. The included EC2 `user_data` installs Docker only; deploying the application on the EC2 host is a separate step. Test Compose locally first.

1. Install Terraform and AWS CLI, configure credentials using `aws configure`, and verify the selected AWS account/region.
2. Ensure you have an SSH key pair. The default public-key path is `~/.ssh/id_rsa.pub`. Generate a key if needed, and pass its public-key path using `-var="public_key_path=..."`. Never provide your private key.
3. Set your public IPv4 address in CIDR notation (for example `203.0.113.10/32`) as `allowed_ssh_cidr`. Do not open SSH to `0.0.0.0/0`.
4. AWS instance Free Tier eligibility depends on account, region and plan. Check eligible types before planning; do not assume `t2.micro` or `t3.micro` is eligible for your account. For example:

   ```powershell
   aws ec2 describe-instance-types --filters Name=free-tier-eligible,Values=true --query "InstanceTypes[].InstanceType" --output text
   ```

5. Run Terraform from the `terraform` directory:

   ```powershell
   cd terraform
   terraform init
   terraform fmt -check
   terraform validate
   terraform plan -var="allowed_ssh_cidr=YOUR_PUBLIC_IP/32" -var="instance_type=YOUR_ELIGIBLE_INSTANCE_TYPE"
   ```

   Review the plan and expected charges before using `terraform apply`. This plan provisions an EC2 instance plus ECR and S3 resources. **Creating EC2 can incur charges even when idle.** Optional RDS is disabled by default; enabling it may incur additional costs and requires a secure `TF_VAR_db_password`.

6. When finished with infrastructure that you created using this Terraform state, run `terraform destroy` from the same directory after reviewing its plan. This deletes the managed resources, including the S3 bucket only if it is empty; remove its contents first if you used it.

## Important notes / limitations

- This is a starter configuration and should be validated in your environment before a public deployment. I could inspect the public repo, but could not run your local Docker build or deploy AWS infrastructure from here.
- The repository currently uses Spring Boot `4.2.0-M2` in `backend/pom.xml`; Maven must be able to resolve that parent version. If your build fails while resolving the parent POM, review the project's Spring Boot version and use a stable compatible release rather than changing it blindly.
- `application-dev.yml` is used for the local Compose demo. It is not a production security profile. Do not use its development JWT secret in production.
- The Terraform configuration deliberately does not expose Prometheus or Grafana ports publicly. For a cloud-hosted monitoring UI, use a VPN/SSH tunnel or add authenticated TLS/reverse-proxy access rather than opening ports 3000 and 9090 to the internet.
- The current Terraform EC2 configuration only installs Docker; it does not automatically clone or launch the app. The EC2 URL output is an address template, not proof the web application is deployed there.
