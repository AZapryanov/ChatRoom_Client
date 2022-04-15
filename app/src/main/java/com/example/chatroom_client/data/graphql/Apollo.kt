package com.example.chatroom_client.data.graphql

import com.apollographql.apollo3.ApolloClient

val apolloClient = ApolloClient.Builder()
    .serverUrl("http://18.185.109.12:8080/graphql")
    .build()