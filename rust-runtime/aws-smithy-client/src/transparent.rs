/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

use std::marker::PhantomData;
use std::task::{Context, Poll};
use tower::{Layer, Service};

pub struct TransparentService<S, Req>(S, PhantomData<Req>);

impl<S: Clone, Req> Clone for TransparentService<S, Req> {
    fn clone(&self) -> Self {
        todo!()
    }
}

impl<S, Req> TransparentService<S, Req>
where
    S: Service<Req>,
{
    pub fn new(service: S) -> Self {
        Self(service, PhantomData::<Req>)
    }
}

impl<S, Req> Service<Req> for TransparentService<S, Req>
where
    S: Service<Req>,
{
    type Response = S::Response;
    type Error = S::Error;
    type Future = S::Future;

    fn poll_ready(&mut self, cx: &mut Context<'_>) -> Poll<Result<(), Self::Error>> {
        self.0.poll_ready(cx)
    }

    fn call(&mut self, req: Req) -> Self::Future {
        self.0.call(req)
    }
}

pub struct TransparentLayer<S, Req>(PhantomData<S>, PhantomData<Req>);

impl<S, Req> TransparentLayer<S, Req>
where
    S: Service<Req>,
{
    pub fn new() -> Self {
        Self(PhantomData, PhantomData)
    }
}

impl<S, Req> Layer<S> for TransparentLayer<S, Req>
where
    S: Service<Req>,
{
    type Service = TransparentService<S, Req>;

    fn layer(&self, inner: S) -> Self::Service
    where
        S: Service<Req>,
    {
        TransparentService::new(inner)
    }
}
