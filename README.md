# PediUber Backend

Backend do sistema **PediUber**, desenvolvido em Java com Spring Boot e integrado ao **RideFleet Core**.

Este repositório é o principal da solução, pois concentra a lógica de corridas, integração com o Core, banco de dados, RabbitMQ, observabilidade, múltiplas instâncias, load balancer e pipeline CI/CD.

---

## Repositórios relacionados

Backend PediUber:

    https://github.com/john-kauan/pediuber-backend

Frontend PediUber:

    https://github.com/john-kauan/pediuber-frontend

RideFleet Core:

    https://github.com/Matt1211/ridefleet-core-sin142

---

## Tecnologias utilizadas

- Java 21
- Spring Boot
- Spring Web
- Spring Data JPA
- Spring AMQP
- PostgreSQL
- RabbitMQ
- Docker
- Docker Compose
- Nginx Load Balancer
- Spring Actuator
- Micrometer
- Prometheus
- Grafana
- GitHub Actions

---

## Objetivo do projeto

O PediUber é um sistema de corridas integrado ao RideFleet Core.

O sistema permite:

- cadastrar motoristas;
- solicitar corridas;
- acompanhar corridas;
- iniciar e finalizar corridas;
- delegar corridas para o Core quando não houver motorista disponível;
- receber corridas delegadas pelo Core;
- expor métricas no padrão Prometheus;
- executar múltiplas instâncias do backend;
- distribuir requisições usando load balancer;
- validar build, testes e deploy por pipeline CI/CD.

---

## Arquitetura geral

A aplicação completa funciona com os seguintes componentes:

    Frontend Angular
           ↓
    Nginx do Frontend
           ↓
    /api
           ↓
    PediUber Load Balancer
           ↓
    pediuber-backend-1
    pediuber-backend-2
    pediuber-backend-3
           ↓
    PostgreSQL
    RabbitMQ
    RideFleet Core

A observabilidade funciona assim:

    PediUber Backend
           ↓
    /actuator/prometheus
           ↓
    Prometheus do Core
           ↓
    Grafana do Core

---

## Estrutura principal do projeto

    pediuber-backend/
    ├── docker-compose.yml
    ├── .env
    ├── .env.example
    ├── nginx/
    │   └── default.conf
    ├── pediuber-dashboard.json
    ├── README.md
    ├── pediuber/
    │   ├── Dockerfile
    │   ├── pom.xml
    │   └── src/
    │       ├── main/
    │       │   ├── java/
    │       │   └── resources/
    │       └── test/
    │           └── java/
    └── .github/
        └── workflows/
            └── backend-ci-cd.yml

---

## Componentes Docker do PediUber

O `docker-compose.yml` sobe os seguintes serviços:

    pediuber-postgres
    pediuber-rabbitmq
    pediuber-backend-1
    pediuber-backend-2
    pediuber-backend-3
    pediuber-load-balancer

As três instâncias do backend usam o mesmo banco PostgreSQL e o mesmo RabbitMQ.

O load balancer recebe as requisições na porta:

    http://localhost:8082

E distribui para:

    pediuber-backend-1:8080
    pediuber-backend-2:8080
    pediuber-backend-3:8080

---

## Configuração do Core

O RideFleet Core pertence ao professor e deve estar rodando para a integração completa funcionar.

O endereço do Core é configurável pela variável:

    RIDEFLEET_CORE_BASE_URL

O valor padrão usado no ambiente Docker é:

    http://ridefleet-core:8080/api/v1

Caso o container do Core no ambiente do professor tenha outro nome, por exemplo `core`, use:

    http://core:8080/api/v1

Caso o Core esteja rodando diretamente na máquina local, pode ser necessário usar:

    http://host.docker.internal:8080/api/v1

No Linux, se `host.docker.internal` não funcionar, recomenda-se deixar Core e PediUber na mesma rede Docker.

---

## Arquivo .env

Crie um arquivo `.env` na raiz do backend:

    cd ~/pediuber-sin142/pediuber-backend
    nano .env

Exemplo de conteúdo:

    RIDEFLEET_API_KEY=SUA_CHAVE_DO_CORE
    RIDEFLEET_CORE_BASE_URL=http://ridefleet-core:8080/api/v1

A chave real do Core não deve ser enviada ao GitHub.

Se no ambiente do professor o Core usar outro nome de container, altere apenas esta variável:

    RIDEFLEET_CORE_BASE_URL=http://core:8080/api/v1

---

## Configuração no application-docker.properties

No perfil Docker, o backend lê o endereço do Core por variável de ambiente.

Configuração esperada:

    ridefleet.core.base-url=${RIDEFLEET_CORE_BASE_URL:http://ridefleet-core:8080/api/v1}
    ridefleet.group-id=pediuber
    ridefleet.api-key=${RIDEFLEET_API_KEY:}

Isso permite alterar o endereço do Core sem modificar o código-fonte.

---

## Como rodar o RideFleet Core

Clone o repositório do Core:

    git clone https://github.com/Matt1211/ridefleet-core-sin142

Entre na pasta do Core:

    cd ridefleet-core

Suba a stack do Core conforme a documentação do professor.

Depois confira se os containers do Core estão rodando:

    docker ps

O esperado é aparecer algo parecido com:

    ridefleet-core
    ridefleet-prometheus
    ridefleet-grafana
    ridefleet-rabbitmq
    ridefleet-db
    ridefleet-redis

Confira também se a rede do Core existe:

    docker network ls | grep ridefleet-net

Se a rede não existir, crie manualmente:

    docker network create ridefleet-net

---

## Como rodar o backend PediUber

Entre na pasta do backend:

    cd ~/pediuber-sin142/pediuber-backend

Suba a aplicação:

    docker compose up -d --build

Confira os containers:

    docker compose ps

O esperado é aparecer:

    pediuber-postgres
    pediuber-rabbitmq
    pediuber-backend-1
    pediuber-backend-2
    pediuber-backend-3
    pediuber-load-balancer

---

## Endereços principais

Backend via load balancer:

    http://localhost:8082

Health check:

    http://localhost:8082/actuator/health

Métricas Prometheus:

    http://localhost:8082/actuator/prometheus

RabbitMQ do PediUber:

    http://localhost:15673

Prometheus do Core:

    http://localhost:9090

Grafana do Core:

    http://localhost:3000

Frontend PediUber:

    http://localhost:4200

---

## Testar se o backend está funcionando

Health check:

    curl -s http://localhost:8082/actuator/health | jq

O esperado é:

    {
      "status": "UP"
    }

Testar métricas do PediUber:

    curl -s http://localhost:8082/actuator/prometheus | grep "pediuber_"

Listar motoristas:

    curl -s http://localhost:8082/drivers | jq

---

## Criar motoristas de teste

Crie três motoristas para testar corridas locais:

    curl -s -X POST http://localhost:8082/drivers \
    -H "Content-Type: application/json" \
    -d '{
      "name": "Motorista 01",
      "vehicle": "Fiat Argo - ABC1D23",
      "available": true
    }' | jq

    curl -s -X POST http://localhost:8082/drivers \
    -H "Content-Type: application/json" \
    -d '{
      "name": "Motorista 02",
      "vehicle": "Chevrolet Onix - DEF4G56",
      "available": true
    }' | jq

    curl -s -X POST http://localhost:8082/drivers \
    -H "Content-Type: application/json" \
    -d '{
      "name": "Motorista 03",
      "vehicle": "Volkswagen Gol - HIJ7K89",
      "available": true
    }' | jq

Conferir no banco:

    docker compose exec -T postgres psql -U postgres -d pediuber -c "
    SELECT id, name, vehicle, available FROM drivers ORDER BY id;
    "

---

## Solicitar uma corrida local

Com motoristas disponíveis, solicite uma corrida:

    curl -s -X POST http://localhost:8082/rides/request \
    -H "Content-Type: application/json" \
    -d '{
      "passengerName": "Teste Local",
      "origin": {
        "lat": -19.191,
        "lng": -46.245,
        "street": "Rua João Leandro",
        "number": "120",
        "city": "Rio Paranaíba",
        "state": "MG"
      },
      "destination": {
        "lat": -19.204,
        "lng": -46.236,
        "street": "UFV Campus Rio Paranaíba",
        "number": "S/N",
        "city": "Rio Paranaíba",
        "state": "MG"
      }
    }' | jq

Essa corrida deve ser atendida pelo próprio PediUber e deve aumentar a métrica:

    pediuber_rides_local_total

---

## Testar corrida delegada para fora

Para testar delegação para fora, deixe todos os motoristas indisponíveis:

    docker compose exec -T postgres psql -U postgres -d pediuber -c "
    UPDATE drivers SET available = false;
    SELECT id, name, vehicle, available FROM drivers ORDER BY id;
    "

Agora solicite uma corrida:

    curl -s -X POST http://localhost:8082/rides/request \
    -H "Content-Type: application/json" \
    -d '{
      "passengerName": "Teste Delegacao",
      "origin": {
        "lat": -19.191,
        "lng": -46.245,
        "street": "Rua João Leandro",
        "number": "120",
        "city": "Rio Paranaíba",
        "state": "MG"
      },
      "destination": {
        "lat": -19.204,
        "lng": -46.236,
        "street": "UFV Campus Rio Paranaíba",
        "number": "S/N",
        "city": "Rio Paranaíba",
        "state": "MG"
      }
    }' | jq

Mesmo que o Core não encontre outro grupo e a requisição retorne erro, a tentativa de delegação deve aumentar a métrica:

    pediuber_rides_delegated_out_total

Depois deixe os motoristas disponíveis novamente:

    docker compose exec -T postgres psql -U postgres -d pediuber -c "
    UPDATE drivers SET available = true;
    "

---

## Testar corrida recebida do Core

É possível simular o Core atribuindo uma corrida ao PediUber pelo endpoint:

    POST /rides/{rideUuid}/assigned

Comando de teste:

    RIDE_UUID="core-test-$(date +%s)"

    curl -s -X POST "http://localhost:8082/rides/$RIDE_UUID/assigned" \
    -H "Content-Type: application/json" \
    -d '{
      "rideUuid": "'"$RIDE_UUID"'",
      "origin": {
        "lat": -19.191,
        "lng": -46.245,
        "street": "Avenida Principal",
        "number": "100",
        "city": "Rio Paranaíba",
        "state": "MG"
      },
      "destination": {
        "lat": -19.204,
        "lng": -46.236,
        "street": "UFV Campus Rio Paranaíba",
        "number": "S/N",
        "city": "Rio Paranaíba",
        "state": "MG"
      },
      "passengerId": "passenger-core-test",
      "originServiceId": "grupo-teste",
      "logicalTimestamp": 1,
      "lockExpiresAt": "2026-12-31T23:59:59"
    }' -i

Essa chamada deve aumentar a métrica:

    pediuber_rides_delegated_in_total

---

## Endpoints principais

Solicitar corrida:

    POST /rides/request

Acompanhar corrida:

    GET /rides/{id}/tracking

Histórico de corridas:

    GET /rides/history

Iniciar corrida:

    PATCH /rides/{id}/start

Finalizar corrida:

    PATCH /rides/{id}/complete

Listar motoristas:

    GET /drivers

Criar motorista:

    POST /drivers

Alterar disponibilidade:

    PATCH /drivers/{id}/availability

Consultar corrida atual do motorista:

    GET /drivers/{id}/current-ride

Health:

    GET /actuator/health

Métricas:

    GET /actuator/prometheus

---

## Observabilidade

O backend expõe métricas no padrão Prometheus em:

    http://localhost:8082/actuator/prometheus

As métricas customizadas do PediUber são:

    pediuber_rides_local_total
    pediuber_rides_delegated_out_total
    pediuber_rides_delegated_in_total
    pediuber_service_available
    pediuber_service_congested
    pediuber_queue_in_size
    pediuber_queue_out_size
    pediuber_queue_in_dlq_size
    pediuber_queue_out_dlq_size
    pediuber_instance_requests_total

Além dessas, o Spring Actuator e o Micrometer expõem métricas HTTP usadas para latência e throughput:

    http_server_requests_seconds_count
    http_server_requests_seconds_sum
    http_server_requests_seconds_max

---

## Significado das métricas

Corridas locais:

    pediuber_rides_local_total

Conta corridas solicitadas no PediUber e atendidas por motoristas próprios.

Corridas delegadas para fora:

    pediuber_rides_delegated_out_total

Conta tentativas de delegação de corridas para o Core quando o PediUber não possui motorista disponível.

Corridas recebidas por delegação:

    pediuber_rides_delegated_in_total

Conta corridas recebidas do Core por delegação.

Estado do serviço:

    pediuber_service_available
    pediuber_service_congested

Indica se o serviço está disponível ou congestionado.

No projeto, o serviço fica congestionado quando não há motoristas disponíveis.

Filas RabbitMQ:

    pediuber_queue_in_size
    pediuber_queue_out_size
    pediuber_queue_in_dlq_size
    pediuber_queue_out_dlq_size

Indicam o tamanho das filas reais e das filas de erro no RabbitMQ.

Carga por instância:

    pediuber_instance_requests_total

Conta requisições recebidas por cada instância do backend.

---

## RabbitMQ

O projeto utiliza RabbitMQ com filas reais.

Filas principais:

    ride.input.queue
    ride.output.queue

Filas de erro:

    ride.input.dlq
    ride.output.dlq

A interface de gerenciamento do RabbitMQ do PediUber fica em:

    http://localhost:15673

Porta AMQP exposta:

    localhost:5673

---

## Múltiplas instâncias e load balancer

O backend roda com três instâncias:

    pediuber-backend-1
    pediuber-backend-2
    pediuber-backend-3

Cada instância possui uma variável `INSTANCE_ID` diferente.

O Nginx load balancer recebe as requisições em:

    http://localhost:8082

E distribui para as três instâncias.

A configuração do Nginx fica em:

    nginx/default.conf

---

## Validar distribuição de carga

Gere requisições pelo load balancer:

    for i in {1..60}; do
      curl -s http://localhost:8082/drivers > /dev/null
    done

Consulte a métrica:

    curl -s "http://localhost:9090/api/v1/query?query=pediuber_instance_requests_total" | jq

O resultado deve mostrar as três instâncias:

    pediuber-backend-1
    pediuber-backend-2
    pediuber-backend-3

---

## Prometheus do Core

Como o Prometheus pertence à stack do RideFleet Core, é necessário adicionar o job do PediUber no `prometheus.yml` do Core.

Arquivo do Core:

    observability/prometheus/prometheus.yml

Dentro de `scrape_configs`, adicione:

    - job_name: "pediuber-backend"
      static_configs:
        - targets:
            - "pediuber-backend-1:8080"
            - "pediuber-backend-2:8080"
            - "pediuber-backend-3:8080"
      metrics_path: /actuator/prometheus

Depois reinicie o Prometheus:

    docker restart ridefleet-prometheus

Confira os targets:

    curl -s http://localhost:9090/api/v1/targets | jq

As três instâncias devem aparecer como `up`.

---

## Grafana

O dashboard do PediUber está no arquivo:

    pediuber-dashboard.json

Importe no Grafana do Core:

    curl -s -u admin:ridefleet \
    -X POST http://localhost:3000/api/dashboards/db \
    -H "Content-Type: application/json" \
    -d @pediuber-dashboard.json | jq

Acesse:

    http://localhost:3000/d/pediuber-semana5/pediuber-semana-5-observabilidade

Caso a senha do Grafana seja diferente, ajuste o comando.

---

## Consultas úteis no Prometheus

Estado das instâncias:

    up

Carga por instância:

    pediuber_instance_requests_total

Corridas locais:

    pediuber_rides_local_total

Corridas delegadas para fora:

    pediuber_rides_delegated_out_total

Corridas recebidas por delegação:

    pediuber_rides_delegated_in_total

Estado do serviço:

    pediuber_service_available
    pediuber_service_congested

Filas:

    pediuber_queue_in_size
    pediuber_queue_out_size

Throughput:

    rate(http_server_requests_seconds_count{job="pediuber-backend"}[5m])

Latência média:

    rate(http_server_requests_seconds_sum{job="pediuber-backend"}[5m])
    /
    rate(http_server_requests_seconds_count{job="pediuber-backend"}[5m])

---

## Frontend em Docker

Clone o repositório do frontend:

    git clone https://github.com/john-kauan/pediuber-frontend

Entre na pasta:

    cd pediuber-frontend

Construa a imagem:

    docker build -t pediuber-frontend:local .

Rode o container na rede do backend:

    docker run -d \
      --name pediuber-frontend \
      --network pediuber-backend_pediuber-net \
      -p 4200:80 \
      pediuber-frontend:local

Acesse:

    http://localhost:4200

---

## Pipeline CI/CD

O pipeline do backend está em:

    .github/workflows/backend-ci-cd.yml

O pipeline executa:

1. Build da aplicação.
2. Testes unitários.
3. Testes de contrato do Core.
4. Testes de integração com PostgreSQL e RabbitMQ.
5. Build da imagem Docker.
6. Deploy automatizado com Docker Compose.

O deploy roda automaticamente na branch `main` ou manualmente via `workflow_dispatch`.

---

## Testes

Rodar testes localmente:

    cd ~/pediuber-sin142/pediuber-backend/pediuber
    ./mvnw test

Rodar build local:

    ./mvnw clean package

---

## Testes de contrato do Core

O projeto possui testes de contrato para garantir que os DTOs usados na comunicação com o Core mantenham os campos esperados.

Arquivo:

    pediuber/src/test/java/com/pediuber/pediuber/core/contract/CoreContractTests.java

Esses testes verificam os contratos de:

    RideAuctionNotification
    RideAssignment

---

## Testes de integração

O pipeline sobe serviços auxiliares para testes de integração:

    PostgreSQL
    RabbitMQ

O perfil usado no CI é:

    application-ci.properties

Esse perfil configura banco, RabbitMQ e propriedades necessárias para rodar os testes no GitHub Actions.

---

## Deploy automatizado

O deploy do pipeline é executado com Docker Compose.

No GitHub Actions, o deploy:

1. Cria a rede externa `ridefleet-net`.
2. Sobe o ambiente com `docker compose up -d --build`.
3. Aguarda a inicialização.
4. Testa o health pelo load balancer.
5. Testa o endpoint Prometheus.
6. Derruba o ambiente ao final da validação.

Esse deploy funciona como validação automatizada do ambiente.

Em um ambiente real, a mesma etapa poderia ser adaptada para:

- um servidor VPS;
- um runner self-hosted;
- publicação de imagem em Docker Hub;
- publicação de imagem no GitHub Container Registry.

---

## Parar ambiente

Na raiz do backend:

    docker compose down

Para remover volumes também:

    docker compose down -v

Use `-v` apenas se quiser apagar os dados do banco e do RabbitMQ.

---

## Fluxo recomendado para apresentação com o professor

1. Subir o Core do professor.
2. Confirmar que a rede `ridefleet-net` existe.
3. Configurar `.env` do PediUber com a URL correta do Core.
4. Subir o backend PediUber.
5. Criar motoristas de teste.
6. Subir o frontend.
7. Configurar o Prometheus do Core para coletar as três instâncias.
8. Importar o dashboard no Grafana.
9. Testar corrida local.
10. Testar corrida delegada para fora.
11. Testar corrida recebida do Core.
12. Mostrar métricas no Grafana.
13. Mostrar GitHub Actions verde.

Comandos principais:

    cd ~/pediuber-sin142/pediuber-backend
    docker compose up -d --build

    curl -s http://localhost:8082/actuator/health | jq

    cd ~/pediuber-sin142/pediuber-frontend
    docker build -t pediuber-frontend:local .

    docker run -d \
      --name pediuber-frontend \
      --network pediuber-backend_pediuber-net \
      -p 4200:80 \
      pediuber-frontend:local

    http://localhost:4200

---

## Observações importantes

O Core é externo ao backend PediUber.

Por isso, em cada ambiente de teste, o endereço do Core deve ser conferido e ajustado pela variável:

    RIDEFLEET_CORE_BASE_URL

Se o Core do professor usar outro nome de container, ajuste essa variável no `.env`.

O Prometheus e o Grafana pertencem à stack do Core.

Por isso, para visualizar as métricas do PediUber no Grafana do Core, é necessário adicionar o job do PediUber no `prometheus.yml` do Core.

O frontend depende do backend em:

    http://localhost:8082

O backend, por sua vez, usa o load balancer para distribuir as requisições entre as três instâncias.
