package com.example.chatroom_client.data.graphql

import com.apollographql.apollo3.ApolloClient

val apolloClient = ApolloClient.Builder()
    .serverUrl("http://192.168.182.37:8080/graphql")
    .build()